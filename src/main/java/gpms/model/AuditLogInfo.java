package gpms.model;

import java.util.Date;

import com.ebay.xcelite.annotations.Column;
import com.ebay.xcelite.annotations.Row;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({ "rowTotal", "userName", "userFullName", "action",
		"activityDate" })
@Row(colsOrder = { "User Name", "User Full Name", "Audit Action",
		"Activity Date" })
public class AuditLogInfo implements Comparable<AuditLogInfo> {
	@JsonProperty("rowTotal")
	private int rowTotal;

	@JsonProperty("userName")
	@Column(name = "User Name")
	private String userName = new String();

	@JsonProperty("userFullName")
	@Column(name = "User Full Name")
	private String userFullName = new String();

	@JsonProperty("action")
	@Column(name = "Audit Action")
	private String action = new String();

	@JsonProperty("activityDate")
	@Column(name = "Activity Date", dataFormat = "yyyy/MM/dd hh:mm:ss")
	private Date activityDate = new Date();

	public AuditLogInfo() {

	}

	public int getRowTotal() {
		return rowTotal;
	}

	public void setRowTotal(int rowTotal) {
		this.rowTotal = rowTotal;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getUserFullName() {
		return userFullName;
	}

	public void setUserFullName(String userFullName) {
		this.userFullName = userFullName;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public Date getActivityDate() {
		return activityDate;
	}

	public void setActivityDate(Date activityDate) {
		this.activityDate = activityDate;
	}

	@Override
	public int compareTo(AuditLogInfo o) {
		if (getActivityDate() == null || o.getActivityDate() == null)
			return 0;
		return o.getActivityDate().compareTo(getActivityDate()); // Descending
	}

}
