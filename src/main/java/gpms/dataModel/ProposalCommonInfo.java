package gpms.dataModel;

import java.io.Serializable;

import com.fasterxml.jackson.databind.JsonNode;

public class ProposalCommonInfo implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String projectTitle = new String();
	private String usernameBy = new String();
	private Double totalCostsFrom = 0.0;
	private Double totalCostsTo = 0.0;
	private String submittedOnFrom = new String();
	private String submittedOnTo = new String();
	private String proposalStatus = new String();
	private String userRole = new String();

	public ProposalCommonInfo() {

	}

	public ProposalCommonInfo(JsonNode proposalObj) {
		if (proposalObj != null && proposalObj.has("ProjectTitle")) {
			projectTitle = proposalObj.get("ProjectTitle").textValue();
		}
		if (proposalObj != null && proposalObj.has("UsernameBy")) {
			usernameBy = proposalObj.get("UsernameBy").textValue();
		}
		if (proposalObj != null && proposalObj.has("SubmittedOnFrom")) {
			submittedOnFrom = proposalObj.get("SubmittedOnFrom").textValue();
		}
		if (proposalObj != null && proposalObj.has("SubmittedOnTo")) {
			submittedOnTo = proposalObj.get("SubmittedOnTo").textValue();
		}
		if (proposalObj != null && proposalObj.has("TotalCostsFrom")) {
			if (proposalObj.get("TotalCostsFrom").textValue() != null) {
				totalCostsFrom = Double.valueOf(proposalObj.get(
						"TotalCostsFrom").textValue());
			}
		}
		if (proposalObj != null && proposalObj.has("TotalCostsTo")) {
			if (proposalObj.get("TotalCostsTo").textValue() != null) {
				totalCostsTo = Double.valueOf(proposalObj.get("TotalCostsTo")
						.textValue());
			}
		}
		if (proposalObj != null && proposalObj.has("ProposalStatus")) {
			proposalStatus = proposalObj.get("ProposalStatus").textValue();
		}
		if (proposalObj != null && proposalObj.has("UserRole")) {
			userRole = proposalObj.get("UserRole").textValue();
		}
	}

	public String getProjectTitle() {
		return projectTitle;
	}

	public void setProjectTitle(String projectTitle) {
		this.projectTitle = projectTitle;
	}

	public String getUsernameBy() {
		return usernameBy;
	}

	public void setUsernameBy(String usernameBy) {
		this.usernameBy = usernameBy;
	}

	public Double getTotalCostsFrom() {
		return totalCostsFrom;
	}

	public void setTotalCostsFrom(Double totalCostsFrom) {
		this.totalCostsFrom = totalCostsFrom;
	}

	public Double getTotalCostsTo() {
		return totalCostsTo;
	}

	public void setTotalCostsTo(Double totalCostsTo) {
		this.totalCostsTo = totalCostsTo;
	}

	public String getSubmittedOnFrom() {
		return submittedOnFrom;
	}

	public void setSubmittedOnFrom(String submittedOnFrom) {
		this.submittedOnFrom = submittedOnFrom;
	}

	public String getSubmittedOnTo() {
		return submittedOnTo;
	}

	public void setSubmittedOnTo(String submittedOnTo) {
		this.submittedOnTo = submittedOnTo;
	}

	public String getProposalStatus() {
		return proposalStatus;
	}

	public void setProposalStatus(String proposalStatus) {
		this.proposalStatus = proposalStatus;
	}

	public String getUserRole() {
		return userRole;
	}

	public void setUserRole(String userRole) {
		this.userRole = userRole;
	}

}
