package gpms.pds;

import java.util.HashMap;
import java.util.Map;

public class TaskDefinition {
	
	private String name;
	
	private String code;
	
	private Map<String, AttributePermission> permissions;

	public TaskDefinition() {
		this.permissions = new HashMap<String, AttributePermission>();
	}

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

	@Override
	public String toString() {
		return "TaskDefinition [name=" + name + ", code=" + code + ", permissions=" + permissions + "]";
	}
	
	
	
	
	
	

}
