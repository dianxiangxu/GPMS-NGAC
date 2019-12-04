package gpms.model;

import java.util.ArrayList;
import java.util.List;

public class RequiredSignaturesInfo {
	private List<String> requiredPISign = new ArrayList<String>();
	private List<String> requiredCoPISigns = new ArrayList<String>();
	private List<String> requiredChairSigns = new ArrayList<String>();
	private List<String> requiredBusinessManagerSigns = new ArrayList<String>();
	private List<String> requiredDeanSigns = new ArrayList<String>();
	private List<String> requiredIRBSigns = new ArrayList<String>();
	private List<String> requiredResearchAdminSigns = new ArrayList<String>();
	private List<String> requiredResearchDirectorSigns = new ArrayList<String>();

	private List<String> existingPISign = new ArrayList<String>();
	private List<String> existingCoPISigns = new ArrayList<String>();
	private List<String> existingChairSigns = new ArrayList<String>();
	private List<String> existingBusinessManagerSigns = new ArrayList<String>();
	private List<String> existingDeanSigns = new ArrayList<String>();
	private List<String> existingIRBSigns = new ArrayList<String>();
	private List<String> existingResearchAdminSigns = new ArrayList<String>();
	private List<String> existingResearchDirectorSigns = new ArrayList<String>();

	private boolean signedByPI = false;
	private boolean signedByAllCoPIs = false;
	private boolean signedByAllChairs = false;
	private boolean signedByAllBusinessManagers = false;
	private boolean signedByAllDeans = false;
	private boolean signedByAllIRBs = false;
	private boolean signedByAllResearchAdmins = false;
	private boolean signedByAllResearchDirectors = false;

	public RequiredSignaturesInfo() {

	}

	public List<String> getRequiredPISign() {
		return requiredPISign;
	}

	public void setRequiredPISign(List<String> requiredPISign) {
		this.requiredPISign = requiredPISign;
	}

	public List<String> getRequiredCoPISigns() {
		return requiredCoPISigns;
	}

	public void setRequiredCoPISigns(List<String> requiredCoPISigns) {
		this.requiredCoPISigns = requiredCoPISigns;
	}

	public List<String> getRequiredChairSigns() {
		return requiredChairSigns;
	}

	public void setRequiredChairSigns(List<String> requiredChairSigns) {
		this.requiredChairSigns = requiredChairSigns;
	}

	public List<String> getRequiredBusinessManagerSigns() {
		return requiredBusinessManagerSigns;
	}

	public void setRequiredBusinessManagerSigns(
			List<String> requiredBusinessManagerSigns) {
		this.requiredBusinessManagerSigns = requiredBusinessManagerSigns;
	}

	public List<String> getRequiredDeanSigns() {
		return requiredDeanSigns;
	}

	public void setRequiredDeanSigns(List<String> requiredDeanSigns) {
		this.requiredDeanSigns = requiredDeanSigns;
	}

	public List<String> getRequiredIRBSigns() {
		return requiredIRBSigns;
	}

	public void setRequiredIRBSigns(List<String> requiredIRBSigns) {
		this.requiredIRBSigns = requiredIRBSigns;
	}

	public List<String> getRequiredResearchAdminSigns() {
		return requiredResearchAdminSigns;
	}

	public void setRequiredResearchAdminSigns(
			List<String> requiredResearchAdminSigns) {
		this.requiredResearchAdminSigns = requiredResearchAdminSigns;
	}

	public List<String> getRequiredResearchDirectorSigns() {
		return requiredResearchDirectorSigns;
	}

	public void setRequiredResearchDirectorSigns(
			List<String> requiredResearchDirectorSigns) {
		this.requiredResearchDirectorSigns = requiredResearchDirectorSigns;
	}

	public List<String> getExistingPISign() {
		return existingPISign;
	}

	public void setExistingPISign(List<String> existingPISign) {
		this.existingPISign = existingPISign;
	}

	public List<String> getExistingCoPISigns() {
		return existingCoPISigns;
	}

	public void setExistingCoPISigns(List<String> existingCoPISigns) {
		this.existingCoPISigns = existingCoPISigns;
	}

	public List<String> getExistingChairSigns() {
		return existingChairSigns;
	}

	public void setExistingChairSigns(List<String> existingChairSigns) {
		this.existingChairSigns = existingChairSigns;
	}

	public List<String> getExistingBusinessManagerSigns() {
		return existingBusinessManagerSigns;
	}

	public void setExistingBusinessManagerSigns(
			List<String> existingBusinessManagerSigns) {
		this.existingBusinessManagerSigns = existingBusinessManagerSigns;
	}

	public List<String> getExistingDeanSigns() {
		return existingDeanSigns;
	}

	public void setExistingDeanSigns(List<String> existingDeanSigns) {
		this.existingDeanSigns = existingDeanSigns;
	}

	public List<String> getExistingIRBSigns() {
		return existingIRBSigns;
	}

	public void setExistingIRBSigns(List<String> existingIRBSigns) {
		this.existingIRBSigns = existingIRBSigns;
	}

	public List<String> getExistingResearchAdminSigns() {
		return existingResearchAdminSigns;
	}

	public void setExistingResearchAdminSigns(
			List<String> existingResearchAdminSigns) {
		this.existingResearchAdminSigns = existingResearchAdminSigns;
	}

	public List<String> getExistingResearchDirectorSigns() {
		return existingResearchDirectorSigns;
	}

	public void setExistingResearchDirectorSigns(
			List<String> existingResearchDirectorSigns) {
		this.existingResearchDirectorSigns = existingResearchDirectorSigns;
	}

	public boolean isSignedByPI() {
		return signedByPI;
	}

	public void setSignedByPI(boolean signedByPI) {
		this.signedByPI = signedByPI;
	}

	public boolean isSignedByAllCoPIs() {
		return signedByAllCoPIs;
	}

	public void setSignedByAllCoPIs(boolean signedByAllCoPIs) {
		this.signedByAllCoPIs = signedByAllCoPIs;
	}

	public boolean isSignedByAllChairs() {
		return signedByAllChairs;
	}

	public void setSignedByAllChairs(boolean signedByAllChairs) {
		this.signedByAllChairs = signedByAllChairs;
	}

	public boolean isSignedByAllBusinessManagers() {
		return signedByAllBusinessManagers;
	}

	public void setSignedByAllBusinessManagers(
			boolean signedByAllBusinessManagers) {
		this.signedByAllBusinessManagers = signedByAllBusinessManagers;
	}

	public boolean isSignedByAllDeans() {
		return signedByAllDeans;
	}

	public void setSignedByAllDeans(boolean signedByAllDeans) {
		this.signedByAllDeans = signedByAllDeans;
	}

	public boolean isSignedByAllIRBs() {
		return signedByAllIRBs;
	}

	public void setSignedByAllIRBs(boolean signedByAllIRBs) {
		this.signedByAllIRBs = signedByAllIRBs;
	}

	public boolean isSignedByAllResearchAdmins() {
		return signedByAllResearchAdmins;
	}

	public void setSignedByAllResearchAdmins(boolean signedByAllResearchAdmins) {
		this.signedByAllResearchAdmins = signedByAllResearchAdmins;
	}

	public boolean isSignedByAllResearchDirectors() {
		return signedByAllResearchDirectors;
	}

	public void setSignedByAllResearchDirectors(
			boolean signedByAllResearchDirectors) {
		this.signedByAllResearchDirectors = signedByAllResearchDirectors;
	}

}