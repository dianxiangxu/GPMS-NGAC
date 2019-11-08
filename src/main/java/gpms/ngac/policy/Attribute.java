package gpms.ngac.policy;

import gov.nist.csd.pm.pip.graph.model.nodes.NodeType;

/**
 * @author Md Nazmul Karim
 *
 *This class is the structure equivalent to json object
 *				{
					"attribute_name" : "PI",
					"type" : "OA",
				
				}	
 */
public class Attribute {

	private String attributeName;
	
	private NodeType attributeType;
	
	public Attribute() {
		
	}
	
	public Attribute(String name, NodeType type) {
		this.attributeName = name;
		this.attributeType = type;
		
	}

	public String getAttributeName() {
		return attributeName;
	}

	public void setAttributeName(String attributeName) {
		this.attributeName = attributeName;
	}

	public NodeType getAttributeType() {
		return attributeType;
	}

	public void setAttributeType(NodeType attributeType) {
		this.attributeType = attributeType;
	}

	

	@Override
	public String toString() {
		return "Permission [attributeName=" + attributeName
				+ ", attributeType=" + attributeType  + "]";
	}
	
	
	
	
	
	
}
