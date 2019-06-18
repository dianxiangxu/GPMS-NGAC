package gpms.model;

import java.io.Serializable;

import com.fasterxml.jackson.databind.JsonNode;

public class GPMSCommonInfo implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private String userProfileID = new String();
	private String userName = new String();
	private boolean userIsAdmin = false;
	private String userCollege = new String();
	private String userDepartment = new String();
	private String userPositionType = new String();
	private String userPositionTitle = new String();
	private Boolean userIsActive = null;

	public GPMSCommonInfo() {

	}

	public GPMSCommonInfo(JsonNode commonObj) {
		if (commonObj != null && commonObj.has("UserProfileID")) {
			userProfileID = commonObj.get("UserProfileID").textValue();
		}
		if (commonObj != null && commonObj.has("UserName")) {
			userName = commonObj.get("UserName").textValue();
		}
		if (commonObj != null && commonObj.has("UserIsAdmin")) {
			userIsAdmin = Boolean.parseBoolean(commonObj.get("UserIsAdmin")
					.textValue());
		}
		if (commonObj != null && commonObj.has("UserCollege")) {
			userCollege = commonObj.get("UserCollege").textValue();
		}
		if (commonObj != null && commonObj.has("UserDepartment")) {
			userDepartment = commonObj.get("UserDepartment").textValue();
		}
		if (commonObj != null && commonObj.has("UserPositionType")) {
			userPositionType = commonObj.get("UserPositionType").textValue();
		}
		if (commonObj != null && commonObj.has("UserPositionTitle")) {
			userPositionTitle = commonObj.get("UserPositionTitle").textValue();
		}
	}

	@Override
	public String toString() {
		return "GPMSCommonInfo [userProfileID=" + userProfileID + ", userName=" + userName + ", userIsAdmin="
				+ userIsAdmin + ", userCollege=" + userCollege + ", userDepartment=" + userDepartment
				+ ", userPositionType=" + userPositionType + ", userPositionTitle=" + userPositionTitle
				+ ", userIsActive=" + userIsActive + "]";
	}

	public String getUserProfileID() {
		return userProfileID;
	}

	public void setUserProfileID(String userProfileID) {
		this.userProfileID = userProfileID;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public boolean isUserIsAdmin() {
		return userIsAdmin;
	}

	public void setUserIsAdmin(boolean userIsAdmin) {
		this.userIsAdmin = userIsAdmin;
	}

	public String getUserCollege() {
		return userCollege;
	}

	public void setUserCollege(String userCollege) {
		this.userCollege = userCollege;
	}

	public String getUserDepartment() {
		return userDepartment;
	}

	public void setUserDepartment(String userDepartment) {
		this.userDepartment = userDepartment;
	}

	public String getUserPositionType() {
		return userPositionType;
	}

	public void setUserPositionType(String userPositionType) {
		this.userPositionType = userPositionType;
	}

	public String getUserPositionTitle() {
		return userPositionTitle;
	}

	public void setUserPositionTitle(String userPositionTitle) {
		this.userPositionTitle = userPositionTitle;
	}

	public Boolean getUserIsActive() {
		return userIsActive;
	}

	public void setUserIsActive(Boolean userIsActive) {
		this.userIsActive = userIsActive;
	}

	public static GPMSCommonInfo getUserBindInfo(JsonNode userObj) {
		GPMSCommonInfo userInfo = new GPMSCommonInfo();
		if (userObj != null && userObj.has("UserName")) {
			userInfo.setUserName(userObj.get("UserName").textValue());
		}
		if (userObj != null && userObj.has("College")) {
			userInfo.setUserCollege(userObj.get("College").textValue());
		}
		if (userObj != null && userObj.has("Department")) {
			userInfo.setUserDepartment(userObj.get("Department").textValue());
		}
		if (userObj != null && userObj.has("PositionType")) {
			userInfo.setUserPositionType(userObj.get("PositionType")
					.textValue());
		}
		if (userObj != null && userObj.has("PositionTitle")) {
			userInfo.setUserPositionTitle(userObj.get("PositionTitle")
					.textValue());
		}
		if (userObj != null && userObj.has("IsActive")) {
			if (!userObj.get("IsActive").isNull()) {
				userInfo.setUserIsActive(userObj.get("IsActive").booleanValue());
			} else {
				userInfo.setUserIsActive(null);
			}
		}
		return userInfo;
	}
}
