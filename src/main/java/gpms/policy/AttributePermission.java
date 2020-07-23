package gpms.policy;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Md Nazmul Karim
 *
 *This class is the structure equivalent to json object
 *				{
					"attribute_name" : "PI",
					"type" : "OA",
					"access_right_set" : ["assign", "assign to"]
				}
	This json object represents a permission in docs/defined_task.json			
 */
public class AttributePermission {

	private String attributeName;
	
	//this will hold attribute_name+ type and together will form a key
	// e.g., PIOA
	private String attributeNameKey;
	
	private String attributeType;
	
	// This variable will store set of access rights as string literals;
	//e.g., "assign", "assign to"
	private Set<String> accessRightsSet;
	
	public AttributePermission() {
		accessRightsSet = new HashSet<String>(); 
	}
	
	public AttributePermission(String name, String type) {
		this.attributeName = name;
		this.attributeType = type;
		this.attributeType = this.attributeName+this.attributeType;
		accessRightsSet = new HashSet<String>(); 
	}

	public String getAttributeName() {
		return attributeName;
	}

	public void setAttributeName(String attributeName) {
		this.attributeName = attributeName;
	}

	public String getAttributeType() {
		return attributeType;
	}

	public void setAttributeType(String attributeType) {
		this.attributeType = attributeType;
	}

	public Set getAccessRightsSet() {
		return accessRightsSet;
	}

	public void setAccessRightsSet(Set accessRightsSet) {
		this.accessRightsSet = accessRightsSet;
	}
	
	public void setAttributeNameKey() {
		this.attributeNameKey = this.attributeName+ this.attributeType;
	}
	
	public String getAttributeNameKey() {
		return this.attributeNameKey;
	}
	
	public void addAccessRight(String arsName) {
		this.accessRightsSet.add(arsName);
	}
	
	public void removeAccessRight(String arsName) {
		this.accessRightsSet.remove(arsName);
	}

	@Override
	public String toString() {
		return "Permission [attributeName=" + attributeName + ", attributeNameKey=" + attributeNameKey
				+ ", attributeType=" + attributeType + ", accessRightsSet=" + accessRightsSet + "]";
	}
	
	
	
	
	
	
}
