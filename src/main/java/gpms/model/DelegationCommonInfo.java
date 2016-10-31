package gpms.model;

import java.io.Serializable;

import com.fasterxml.jackson.databind.JsonNode;

public class DelegationCommonInfo implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private String delegatee = new String();
	private String createdFrom = new String();
	private String createdTo = new String();
	private String delegatedAction = new String();
	private Boolean isRevoked = null;

	public DelegationCommonInfo() {

	}

	public DelegationCommonInfo(JsonNode delegationObj) {
		if (delegationObj != null && delegationObj.has("delegatee")) {
			delegatee = delegationObj.get("delegatee").textValue();
		}
		if (delegationObj != null && delegationObj.has("createdFrom")) {
			createdFrom = delegationObj.get("createdFrom").textValue();
		}
		if (delegationObj != null && delegationObj.has("createdTo")) {
			createdTo = delegationObj.get("createdTo").textValue();
		}
		if (delegationObj != null && delegationObj.has("delegatedAction")) {
			delegatedAction = delegationObj.get("delegatedAction").textValue();
		}
		if (delegationObj != null && delegationObj.has("isRevoked")) {
			if (!delegationObj.get("isRevoked").isNull()) {
				isRevoked = delegationObj.get("isRevoked").booleanValue();
			} else {
				isRevoked = null;
			}
		}
	}

	public String getDelegatee() {
		return delegatee;
	}

	public void setDelegatee(String delegatee) {
		this.delegatee = delegatee;
	}

	public String getCreatedFrom() {
		return createdFrom;
	}

	public void setCreatedFrom(String createdFrom) {
		this.createdFrom = createdFrom;
	}

	public String getCreatedTo() {
		return createdTo;
	}

	public void setCreatedTo(String createdTo) {
		this.createdTo = createdTo;
	}

	public String getDelegatedAction() {
		return delegatedAction;
	}

	public void setDelegatedAction(String delegatedAction) {
		this.delegatedAction = delegatedAction;
	}

	public Boolean getIsRevoked() {
		return isRevoked;
	}

	public void setIsRevoked(Boolean isRevoked) {
		this.isRevoked = isRevoked;
	}

}
