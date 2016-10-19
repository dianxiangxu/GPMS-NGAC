package gpms.model;

import java.io.Serializable;

import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Property;
import org.mongodb.morphia.annotations.Reference;

@Embedded
public class InvestigatorRefAndPosition implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Reference(value = "user profile" /* , lazy = true */)
	UserProfile userRef = new UserProfile();

	@Property("user profile id")
	private String userProfileId = new String();

	@Property("college")
	private String college = new String();

	@Property("department")
	private String department = new String();

	@Property("position type")
	private String positionType = new String();

	@Property("position title")
	private String positionTitle = new String();

	public InvestigatorRefAndPosition() {

	}

	public UserProfile getUserRef() {
		return userRef;
	}

	public void setUserRef(UserProfile userRef) {
		this.userRef = userRef;
	}

	public String getUserProfileId() {
		return userProfileId;
	}

	public void setUserProfileId(String userProfileId) {
		this.userProfileId = userProfileId;
	}

	public String getCollege() {
		return college;
	}

	public void setCollege(String college) {
		this.college = college;
	}

	public String getDepartment() {
		return department;
	}

	public void setDepartment(String department) {
		this.department = department;
	}

	public String getPositionType() {
		return positionType;
	}

	public void setPositionType(String positionType) {
		this.positionType = positionType;
	}

	public String getPositionTitle() {
		return positionTitle;
	}

	public void setPositionTitle(String positionTitle) {
		this.positionTitle = positionTitle;
	}

	@Override
	public String toString() {
		return "InvestigatorRefAndPosition [userRef=" + userRef
				+ ", userProfileId=" + userProfileId + ", college=" + college
				+ ", department=" + department + ", positionType="
				+ positionType + ", positionTitle=" + positionTitle + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((college == null) ? 0 : college.hashCode());
		result = prime * result
				+ ((department == null) ? 0 : department.hashCode());
		result = prime * result
				+ ((positionTitle == null) ? 0 : positionTitle.hashCode());
		result = prime * result
				+ ((positionType == null) ? 0 : positionType.hashCode());
		result = prime * result
				+ ((userProfileId == null) ? 0 : userProfileId.hashCode());
		result = prime * result + ((userRef == null) ? 0 : userRef.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		InvestigatorRefAndPosition other = (InvestigatorRefAndPosition) obj;
		if (college == null) {
			if (other.college != null)
				return false;
		} else if (!college.equals(other.college))
			return false;
		if (department == null) {
			if (other.department != null)
				return false;
		} else if (!department.equals(other.department))
			return false;
		if (positionTitle == null) {
			if (other.positionTitle != null)
				return false;
		} else if (!positionTitle.equals(other.positionTitle))
			return false;
		if (positionType == null) {
			if (other.positionType != null)
				return false;
		} else if (!positionType.equals(other.positionType))
			return false;
		if (userProfileId == null) {
			if (other.userProfileId != null)
				return false;
		} else if (!userProfileId.equals(other.userProfileId))
			return false;
		if (userRef == null) {
			if (other.userRef != null)
				return false;
		} else if (!userRef.equals(other.userRef))
			return false;
		return true;
	}

}