package gpms.policy;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Md Nazmul Karim
 *
 *This class is a structured definition of a Task as defined in 'docs/defined_tasks.json
 */
public class TaskDefinition {
	
	//Stores name of the task e.g., "Add Co-PI"
	private String name;
	
	// assigns a code for the task
	private String code;
	
	//The task may require 1 or more AttributePermission
	// the key of the map is attributeNameKey= attributeName + attributeType
	private Map<String, AttributePermission> permissions;

	
	/**
	 * map object created
	 */
	public TaskDefinition() {
		this.permissions = new HashMap<String, AttributePermission>();
	}

	/**
	 * @param name is task name; e.g., "Create Proposal"
	 * @param code is a assigned code 
	 */
	public TaskDefinition(String name, String code) {		
		this.name = name;
		this.code = code;		
		this.permissions = new HashMap<String, AttributePermission>();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public Map<String, AttributePermission> getPermissions() {
		return permissions;
	}

	public void setPermissions(Map<String, AttributePermission> permissions) {
		this.permissions = permissions;
	}
	
	
	public void addPermission(AttributePermission per) {
		this.permissions.put(per.getAttributeNameKey(), per);
	}
	
	
	public void removePermission(AttributePermission per) {
		this.permissions.remove(per.getAttributeNameKey());
	}
	
	
	/**
	 * @return a HashMap
	 * the key is attributeNameKey of class AttributePermission
	 * and value set is the access right set.
	 */
	public HashMap<String, HashSet> accumulateAllAccessRightsSet()
	{
		HashMap<String, HashSet> allArs = new HashMap<String,HashSet>();
		
		for (Map.Entry<String,AttributePermission> entry : this.permissions.entrySet()) {
			
			String key = entry.getKey();
			if(allArs.containsKey(key)) {
				
				HashSet set = (HashSet)allArs.get(key);				
				set.addAll(((AttributePermission)entry.getValue()).getAccessRightsSet());
				allArs.put(key,set);
			}
			else {
				
				HashSet set = (HashSet) ((AttributePermission)entry.getValue()).getAccessRightsSet();
				allArs.put(key,set);
			}				
			
		}             
		
		return allArs;
	}

	@Override
	public String toString() {
		return "TaskDefinition [name=" + name + ", code=" + code + ", permissions=" + permissions + "]";
	}
	
	
	
	
	
	
	
	

}
