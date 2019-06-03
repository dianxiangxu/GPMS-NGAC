package gpms.pds;

import java.util.HashSet;
import java.util.Set;

public class AttributePermission {

	private String attributeName;
	
	private String attributeNameKey;
	
	private String attributeType;
	
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
