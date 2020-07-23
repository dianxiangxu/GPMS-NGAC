package gpms.dataModel;

import java.io.Serializable;

import com.fasterxml.jackson.databind.JsonNode;

public class AuditLogCommonInfo implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String action = new String();
	private String auditedBy = new String();
	private String activityOnFrom = new String();
	private String activityOnTo = new String();

	public AuditLogCommonInfo() {

	}

	public AuditLogCommonInfo(JsonNode auditLogBindObj) {
		if (auditLogBindObj != null && auditLogBindObj.has("Action")) {
			action = auditLogBindObj.get("Action").textValue();
		}
		if (auditLogBindObj != null && auditLogBindObj.has("AuditedBy")) {
			auditedBy = auditLogBindObj.get("AuditedBy").textValue();
		}
		if (auditLogBindObj != null && auditLogBindObj.has("ActivityOnFrom")) {
			activityOnFrom = auditLogBindObj.get("ActivityOnFrom").textValue();
		}
		if (auditLogBindObj != null && auditLogBindObj.has("ActivityOnTo")) {
			activityOnTo = auditLogBindObj.get("ActivityOnTo").textValue();
		}
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public String getAuditedBy() {
		return auditedBy;
	}

	public void setAuditedBy(String auditedBy) {
		this.auditedBy = auditedBy;
	}

	public String getActivityOnFrom() {
		return activityOnFrom;
	}

	public void setActivityOnFrom(String activityOnFrom) {
		this.activityOnFrom = activityOnFrom;
	}

	public String getActivityOnTo() {
		return activityOnTo;
	}

	public void setActivityOnTo(String activityOnTo) {
		this.activityOnTo = activityOnTo;
	}

}
