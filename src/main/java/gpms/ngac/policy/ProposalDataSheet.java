package gpms.ngac.policy;

import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.O;
import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.OA;
import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.U;
import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.UA;
import gpms.ngac.policy.ProposalStage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import gov.nist.csd.pm.exceptions.PMException;
import gov.nist.csd.pm.pip.graph.Graph;
import gov.nist.csd.pm.pip.graph.model.nodes.Node;
import gov.nist.csd.pm.pip.graph.model.nodes.NodeType;
import gov.nist.csd.pm.pip.prohibitions.MemProhibitions;
import gov.nist.csd.pm.pip.prohibitions.Prohibitions;
import gpms.DAL.DepartmentsPositionsCollection;
import gpms.model.InvestigatorInfo;
import gpms.model.InvestigatorRefAndPosition;
import gpms.model.Proposal;
import gpms.model.SubmitType;

public class ProposalDataSheet {
	

	private static final Logger log = Logger.getLogger(ProposalDataSheet.class.getName());
	
	private Proposal proposalData;
	
	private Graph proposalPolicy;
	
	private Prohibitions prohibitions = null;
	NGACPolicyConfigurationLoader policyLoader;
	
	private HashSet<String> involvedDepartments;
	
	
	private ProposalStage proposalStage;
	
	public ProposalDataSheet() {
		prohibitions = new MemProhibitions();
		policyLoader = new NGACPolicyConfigurationLoader();	
		involvedDepartments = new HashSet<String>();
		
	}
	
	public Proposal getProposal() {
		return proposalData;
	}

	public void setProposal(Proposal proposal) {
		this.proposalData = proposal;
		calculateStageofTheProposal();
	}

	public Graph getProposalPolicy() {
		return proposalPolicy;
	}
	
	public Prohibitions getProhibitions() {
		return prohibitions;
	}
	
	public void generatePostSubmissionProhibitions() {
		if(PDSOperations.proposalProhibitions ==null) {
			PDSOperations.proposalProhibitions = policyLoader.loadProhibitions(Constants.PROHIBITION_POST_SUBMISSION);
		    this.prohibitions = PDSOperations.proposalProhibitions;
		}
		else {
			this.prohibitions = PDSOperations.proposalProhibitions;
		}
	}

	public void setProposalPolicy(Graph policy) {
		proposalPolicy = policy;		
	}
	
	public void calculateStageofTheProposal() {
		if(proposalData != null) {
			if(proposalData.getSubmittedByPI().equals(SubmitType.SUBMITTED)){
				generatePostSubmissionProhibitions();
			}
		}
	}
	
	public void checkStaticACRights() {
//		System.out.println("Nazmul:"+UserPermissionChecker.checkPermission(proposalPolicy, getProhibitions(), "nazmul", new Attribute("PI-Editable-Data",NodeType.OA),   new String[] {"w"}  ));
//		System.out.println("Liliana:"+UserPermissionChecker.checkPermission(proposalPolicy, getProhibitions(), "liliana", new Attribute("CoPI-Editable-Data",NodeType.OA),   new String[] {"w"}  ));
//		System.out.println("tomtom:"+UserPermissionChecker.checkPermission(proposalPolicy, getProhibitions(), "tomtom", new Attribute("Data-Element",NodeType.OA),   new String[] {"r"}  ));
		
	}
	
	public ProposalStage getProposalStage() {
		return proposalStage;
	}

	public void setProposalStage(ProposalStage proposalStage) {
		this.proposalStage = proposalStage;
	}
	
	public void clearIngestigator() {
		try {
			long userAttPINodeID = PDSOperations.getNodeID(proposalPolicy, "PI", NodeType.UA, null);
			Set<Long> childIds = proposalPolicy.getChildren(userAttPINodeID);
			log.info("PI deassign:"+childIds.size());
			for (long id : childIds) {
				proposalPolicy.deassign(id, userAttPINodeID);
			}
			
			long userAttCoPINodeID = PDSOperations.getNodeID(proposalPolicy, "CoPI", NodeType.UA, null);
			childIds.clear();
			childIds = proposalPolicy.getChildren(userAttCoPINodeID);
			log.info("CoPI deassign:"+childIds.size());
			for (long id : childIds) {
				proposalPolicy.deassign(id, userAttCoPINodeID);
			}
			
			long userAttSPNodeID = PDSOperations.getNodeID(proposalPolicy, "SP", NodeType.UA, null);
			childIds.clear();
			childIds = proposalPolicy.getChildren(userAttSPNodeID);
			log.info("SP deassign:"+childIds.size());
			for (long id : childIds) {
				proposalPolicy.deassign(id, userAttSPNodeID);
			}
			
		} catch (PMException e) {
			e.printStackTrace();
		}
	}
	
	public void clearAdmins() {
		try {
			long userAttNodeID = PDSOperations.getNodeID(proposalPolicy, Constants.CHAIR_UA, NodeType.UA, null);
			Set<Long> childIds = proposalPolicy.getChildren(userAttNodeID);
			log.info("Chair Clear:"+childIds.size());
			for (long id : childIds) {
				proposalPolicy.deassign(id, userAttNodeID);
			}
			
			long userAttBmNodeID = PDSOperations.getNodeID(proposalPolicy, Constants.BM_UA , NodeType.UA, null);
			childIds.clear();
			childIds = proposalPolicy.getChildren(userAttBmNodeID);
			log.info("BM Clear:"+childIds.size());
			for (long id : childIds) {
				proposalPolicy.deassign(id, userAttBmNodeID);
			}
			
			long userAttDeanNodeID = PDSOperations.getNodeID(proposalPolicy, Constants.DEAN_UA, NodeType.UA, null);
			childIds.clear();
			childIds = proposalPolicy.getChildren(userAttDeanNodeID);
			log.info("Dean Clear:"+childIds.size());
			for (long id : childIds) {
				proposalPolicy.deassign(id, userAttDeanNodeID);
			}
			
			long userAttIrbNodeID = PDSOperations.getNodeID(proposalPolicy, Constants.IRB_UA, NodeType.UA, null);
			childIds.clear();
			childIds = proposalPolicy.getChildren(userAttIrbNodeID);
			log.info("IRB Clear:"+childIds.size());
			for (long id : childIds) {
				proposalPolicy.deassign(id, userAttIrbNodeID);
			}
			
			long userAttRaNodeID = PDSOperations.getNodeID(proposalPolicy, Constants.RA_UA, NodeType.UA, null);
			childIds.clear();
			childIds = proposalPolicy.getChildren(userAttRaNodeID);
			log.info("RA Clear:"+childIds.size());
			for (long id : childIds) {
				proposalPolicy.deassign(id, userAttRaNodeID);
			}
			
			long userAttRdNodeID = PDSOperations.getNodeID(proposalPolicy, Constants.RD_UA, NodeType.UA, null);
			childIds.clear();
			childIds = proposalPolicy.getChildren(userAttRdNodeID);
			log.info("RD Clear:"+childIds.size());
			for (long id : childIds) {
				proposalPolicy.deassign(id, userAttRdNodeID);
			}
			
			
		} catch (PMException e) {
			e.printStackTrace();
		}
	}
	
	public void updatePostSubmissionchanges() {
		updatePostSubmissionUsers();
		// associate relations
		associateApprovalRelation();
		// load create approval PC
		
		testUsersAfterSubmission();
	}
	
	private void associateApprovalRelation() {
		try {
			long userAttNodeID = PDSOperations.getNodeID(proposalPolicy, Constants.CHAIR_UA, NodeType.UA, null);
			long oaChairApprovalAttNodeID = PDSOperations.getNodeID(proposalPolicy, Constants.CHAIR_APPROVAL, NodeType.OA, null);
			long oaCompositPDSAttNodeID = PDSOperations.getNodeID(proposalPolicy, Constants.PDSs_OA_UA_LBL, NodeType.OA, null);
			long oaSignatureInfoAttNodeID = PDSOperations.getNodeID(proposalPolicy, Constants.SIGNATURE_INFOL, NodeType.OA, null);
			
			proposalPolicy.associate(userAttNodeID, oaChairApprovalAttNodeID, new HashSet<>(Arrays.asList("assign-o")));
			proposalPolicy.associate(userAttNodeID, oaCompositPDSAttNodeID, new HashSet<>(Arrays.asList("read","Approve","Disapprove")));
			proposalPolicy.associate(userAttNodeID, oaSignatureInfoAttNodeID, new HashSet<>(Arrays.asList("w")));
			
		} catch(PMException e) {
			e.printStackTrace();
		}
	}
	
	public boolean isEmpty(String target, String type) {

		boolean ret = false;
		long targetNodeId = 0;
		try {
			if (type.equals("OA")) {
				targetNodeId = PDSOperations.getNodeID(proposalPolicy, target, NodeType.OA, null);

			} else if (type.equals("UA")) {
				targetNodeId = PDSOperations.getNodeID(proposalPolicy, target, NodeType.UA, null);

			}
			Set<Long> childIds = proposalPolicy.getChildren(targetNodeId);
			if (childIds.isEmpty()) {
				ret = true;
			}
		} catch (PMException e) {
			e.printStackTrace();
		}

		return ret;
	}
	
	public void testUsersAfterSubmission() {
		try {
			long userAttNodeID = PDSOperations.getNodeID(proposalPolicy, Constants.CHAIR_UA, NodeType.UA, null);
			Set<Long> childIds = proposalPolicy.getChildren(userAttNodeID);
			log.info("Chair now:"+childIds.size());
			for (long id : childIds) {
				log.info(proposalPolicy.getNode(id).getName());
			}
			
			long userAttBmNodeID = PDSOperations.getNodeID(proposalPolicy, Constants.BM_UA , NodeType.UA, null);
			childIds.clear();
			childIds = proposalPolicy.getChildren(userAttBmNodeID);
			log.info("BM now:"+childIds.size());
			for (long id : childIds) {
				log.info(proposalPolicy.getNode(id).getName());
			}
			
			long userAttDeanNodeID = PDSOperations.getNodeID(proposalPolicy, Constants.DEAN_UA, NodeType.UA, null);
			childIds.clear();
			childIds = proposalPolicy.getChildren(userAttDeanNodeID);
			log.info("Dean now:"+childIds.size());
			for (long id : childIds) {
				log.info(proposalPolicy.getNode(id).getName());
			}
			
			long userAttIrbNodeID = PDSOperations.getNodeID(proposalPolicy, Constants.IRB_UA, NodeType.UA, null);
			childIds.clear();
			childIds = proposalPolicy.getChildren(userAttIrbNodeID);
			log.info("IRB now:"+childIds.size());
			for (long id : childIds) {
				log.info(proposalPolicy.getNode(id).getName());
			}
			
			long userAttRaNodeID = PDSOperations.getNodeID(proposalPolicy, Constants.RA_UA, NodeType.UA, null);
			childIds.clear();
			childIds = proposalPolicy.getChildren(userAttRaNodeID);
			log.info("RA now:"+childIds.size());
			for (long id : childIds) {
				log.info(proposalPolicy.getNode(id).getName());
			}
			
			long userAttRdNodeID = PDSOperations.getNodeID(proposalPolicy, Constants.RD_UA, NodeType.UA, null);
			childIds.clear();
			childIds = proposalPolicy.getChildren(userAttRdNodeID);
			log.info("RD now:"+childIds.size());
			for (long id : childIds) {
				proposalPolicy.deassign(id, userAttRdNodeID);
			}
			
			
		} catch (PMException e) {
			e.printStackTrace();
		}
	}
	
	public void updatePostSubmissionUsers() {
		
		clearAdmins();
		try {
			for(String dept : involvedDepartments) {
				long userAttNodeID = PDSOperations.getNodeID(proposalPolicy, Constants.CHAIR_UA, NodeType.UA, null);
				long userBmAttNodeID = PDSOperations.getNodeID(proposalPolicy, Constants.BM_UA, NodeType.UA, null);
				long userDeanAttNodeID = PDSOperations.getNodeID(proposalPolicy, Constants.DEAN_UA, NodeType.UA, null);
				
				long deptChairUser = PDSOperations.getNodeID(proposalPolicy, DepartmentsPositionsCollection.adminUsers.get( DepartmentsPositionsCollection.departmentNames.get(dept)+"CHAIR"), NodeType.U, null);
				long deptBMUser = PDSOperations.getNodeID(proposalPolicy, DepartmentsPositionsCollection.adminUsers.get( DepartmentsPositionsCollection.departmentNames.get(dept)+"BM"), NodeType.U, null);
				long deptDeanUser = PDSOperations.getNodeID(proposalPolicy, DepartmentsPositionsCollection.adminUsers.get( DepartmentsPositionsCollection.departmentNames.get(dept)+"DEAN"), NodeType.U, null);
				
				proposalPolicy.assign(deptChairUser, userAttNodeID);
				proposalPolicy.assign(deptBMUser, userBmAttNodeID);
				proposalPolicy.assign(deptDeanUser, userDeanAttNodeID);
			}
			
			long userIrbAttNodeID = PDSOperations.getNodeID(proposalPolicy, Constants.IRB_UA, NodeType.UA, null);
			long userRaAttNodeID = PDSOperations.getNodeID(proposalPolicy, Constants.RA_UA, NodeType.UA, null);
			long userRdAttNodeID = PDSOperations.getNodeID(proposalPolicy, Constants.RD_UA, NodeType.UA, null);
			
			long irbUser = PDSOperations.getNodeID(proposalPolicy, DepartmentsPositionsCollection.adminUsers.get( "IRB"), NodeType.U, null);
			long raBMUser = PDSOperations.getNodeID(proposalPolicy, DepartmentsPositionsCollection.adminUsers.get( "URD"), NodeType.U, null);
			long rdUser = PDSOperations.getNodeID(proposalPolicy, DepartmentsPositionsCollection.adminUsers.get( "IRB"), NodeType.U, null);
			
			
			proposalPolicy.assign(irbUser, userIrbAttNodeID);
			proposalPolicy.assign(raBMUser, userRaAttNodeID);
			proposalPolicy.assign(rdUser, userRdAttNodeID);
			
		}catch(PMException e) {
			e.printStackTrace();
		}
		
	}

	public void updatePI(boolean updateDept) throws PMException{
		String userName = proposalData.getInvestigatorInfo().getPi().getUserRef().getUserAccount().getUserName();
		
		if(updateDept) {
			involvedDepartments.add(proposalData.getInvestigatorInfo().getPi().getUserRef().getDetails(0).getDepartment().trim());
		}
		log.info("Updating PI:"+userName);
		log.info("No of Nodes:"+proposalPolicy.getNodes().size());
		long PiOANode = PDSOperations.getNodeID(proposalPolicy, Constants.PI_OA_LBL, OA, null);
        long PiUANode = PDSOperations.getNodeID(proposalPolicy, Constants.PI_UA_LBL, UA, null);
	    
		
		long piUNodeId = 0;
        try {
        	piUNodeId = PDSOperations.getNodeID(proposalPolicy,userName , U, null);
        	Node piName =proposalPolicy.createNode(PDSOperations.getID(),userName, O, null);
        	
        	proposalPolicy.assign(piUNodeId, PiUANode);
            proposalPolicy.assign( piName.getID(),PiOANode);   		
        	log.info("PI added.");
        	PDSOperations.proposalPolicies.put(proposalData.getNgacId(), proposalPolicy);
        }catch(PMException e) {
        	e.printStackTrace();
        }
	}
	
	public void updateCoPI(String actor,boolean updateDept) throws PMException{
		//InvestigatorRefAndPosition
		InvestigatorInfo investigatorInfo = proposalData.getInvestigatorInfo();
		ArrayList<InvestigatorRefAndPosition> investList = new ArrayList(investigatorInfo.getCo_pi());
		
		log.info("List of coPI:");
		String coPiName = "";
		
		long coPiOANode = PDSOperations.getNodeID(proposalPolicy, Constants.CO_PI_OA_LBL, OA, null);
        long coPiUANode = PDSOperations.getNodeID(proposalPolicy, Constants.CO_PI_UA_LBL, UA, null);
	    
		for(InvestigatorRefAndPosition investPos : investList)
		{
			log.info(investPos.getPositionTitle());
			coPiName = investPos.getUserRef().getUserAccount().getUserName();
			log.info(coPiName);
			if(updateDept) {
				involvedDepartments.add(investPos.getDepartment());
			}
		
		
	        long copiUNodeId = 0;
	        try {
	        	copiUNodeId = PDSOperations.getNodeID(proposalPolicy,coPiName , U, null);
	        	
	        	//Node coPINode =proposalPolicy.createNode(PDSOperations.getID(),coPiName, U, null);
	        	Node copiName =proposalPolicy.createNode(PDSOperations.getID(),coPiName, O, null);
	        	
	        	//copiUANodeId = coPINode.getID();
	        	proposalPolicy.assign(copiUNodeId, coPiUANode);
	            proposalPolicy.assign(copiName.getID(), coPiOANode);   		
	        	log.info("CoPI added.");
	        	PDSOperations.proposalPolicies.put(proposalData.getNgacId(), proposalPolicy);
	        }catch(PMException e) {
	        	e.printStackTrace();
	        }
		}
	}
	
	public void updateSP(String actor, boolean updateDept) throws PMException{
		//InvestigatorRefAndPosition
		InvestigatorInfo investigatorInfo = proposalData.getInvestigatorInfo();
		ArrayList<InvestigatorRefAndPosition> investList = new ArrayList(investigatorInfo.getSeniorPersonnel());
		
		log.info("List of SP:");
		String spName = "";
		
		long coPiOANode = PDSOperations.getNodeID(proposalPolicy, Constants.SENIOR_PERSON_OA_LBL, OA, null);
        long coPiUANode = PDSOperations.getNodeID(proposalPolicy, Constants.SENIOR_PERSON_UA_LBL, UA, null);
	    
		for(InvestigatorRefAndPosition investPos : investList)
		{
			log.info(investPos.getPositionTitle());
			spName = investPos.getUserRef().getUserAccount().getUserName();
			log.info(spName);
		
			if(updateDept) {
				involvedDepartments.add(investPos.getDepartment());
			}
		
	        long spUNodeId = 0;
	        try {
	        	spUNodeId = PDSOperations.getNodeID(proposalPolicy,spName , U, null);
	        	
	        	//Node spNode =proposalPolicy.createNode(PDSOperations.getID(),spName, U, null);
	        	Node spNodeName =proposalPolicy.createNode(PDSOperations.getID(),spName, O, null);
	        	
	        	//spUANodeId = spNode.getID();
	        	proposalPolicy.assign(spUNodeId, coPiUANode);
	            proposalPolicy.assign(spNodeName.getID(), coPiOANode);   		
	        	log.info("SP added.");
	        	PDSOperations.proposalPolicies.put(proposalData.getNgacId(), proposalPolicy);
	        }catch(PMException e) {
	        	e.printStackTrace();
	        }
		}
	}
	
	public String getPolicyDecision(PDSOperations pdsOps,String username,String action,String objectAtt) {
		
		
		log.info("userName:"+username+"| Action:"+action+"|OA:"+objectAtt);
		String decision ="";
		String[] operations = {action};
		ArrayList<String> ops = new ArrayList<String>();
		ops.addAll(Arrays.asList(operations));		
		Attribute att = new Attribute(objectAtt, NodeType.OA);
		boolean hasPermission = UserPermissionChecker.checkPermission(proposalPolicy, getProhibitions(), username, att,  operations);
		if(hasPermission == true)
			decision = "Permit";
		else
			decision = "Deny";
		return decision;
	}
	
	public String getPolicyDecisionAnyType(PDSOperations pdsOps,String subject,String type, String action,String objectAtt) {
		
		
		log.info("Subject:"+subject+"|type:"+type+" Action:"+action+"|OA:"+objectAtt);
		String decision ="";
		String[] operations = {action};
		ArrayList<String> ops = new ArrayList<String>();
		ops.addAll(Arrays.asList(operations));		
		Attribute att = new Attribute(objectAtt, NodeType.OA);
		boolean hasPermission = UserPermissionChecker.checkPermissionAnyType(proposalPolicy, getProhibitions(),subject, type,att,  ops);
		if(hasPermission == true)
			decision = "Permit";
		else
			decision = "Deny";
		return decision;
	}
	
	
	public List<String> getPermittedActions(PDSOperations pdsOps, String username, String objectAtt) {
		
		ArrayList<String> permittedActions = new ArrayList<String>();
		List<String> ops = Arrays.asList("Save", "Submit",
				"Approve", "Disapprove", "Withdraw", "Archive", "Delete");
		
		//ArrayList<String> ops = new ArrayList<String>();
		//ops.addAll(Arrays.asList(operations));
		
		Attribute att = new Attribute(objectAtt, NodeType.OA);
		
		for(String operation : ops) {
			String[] operations = {operation};
			boolean hasPermission = UserPermissionChecker.checkPermission(proposalPolicy, getProhibitions(), username, att,  operations);
			if(hasPermission == true) {
				permittedActions.add(operation);
				log.info("Permitted Action: "+operation);
			}
				
		}
		
		return permittedActions;
	}
	
	public String setSection(String proposalSection) {
		log.info("PROPOSAL SECTION:"+proposalSection);
		String objectAtt = "";
		if (proposalSection.equalsIgnoreCase("Project Information")) {
			objectAtt = Constants.PROJECT_INFO_OA_LBL;
		}else if(proposalSection.equalsIgnoreCase("InvestigatorInformation")) {
			objectAtt = Constants.INVESTIGATOR_OA_UA_LBL;
		} 		
		else if(proposalSection.equalsIgnoreCase("Certification/Signatures")) {
			objectAtt = Constants.SIGNATURE_INFOL;
		}else if (proposalSection.equalsIgnoreCase("Sponsor And Budget Information")) {
			objectAtt = Constants.BUDGET_INFO_OA_LBL;
		} else if (proposalSection.equalsIgnoreCase("cost share info")) {
			objectAtt = Constants.COST_SHARE_INFO_OA_LBL;
		} else if (proposalSection.equalsIgnoreCase("university commitments")) {
			objectAtt = Constants.UNIV_COMMITMENT_INFO_OA_LBL;
		} else if (proposalSection.equalsIgnoreCase("conflict of interest and commitment info")) {
			objectAtt = Constants.CONFLICT_OF_INTEREST_INFO_OA_LBL;
		} else if (proposalSection.equalsIgnoreCase("compliance info")) {
			objectAtt = Constants.COMPLIANCE_INFO_OA_LBL;
		} else if (proposalSection.equalsIgnoreCase("additional info")) {
			objectAtt = Constants.ADDITIONAL_INFO_OA_LBL;
		} else if (proposalSection.equalsIgnoreCase("collaboration info")) {
			objectAtt = Constants.COLLABORATION_INFO_OA_LBL;
		} else if (proposalSection.equalsIgnoreCase("proprietary/confidential info")) {
			objectAtt = Constants.CONFIDENTIAL_INFO_OA_LBL;
		} else if (proposalSection.equalsIgnoreCase("osp section info")) {
			objectAtt = Constants.OSP_SECTION_INFO_OA_LBL;
		} else if (proposalSection.equalsIgnoreCase("signature info")) {
			objectAtt = Constants.SIGNATURE_INFO_OA_LBL;
		} else {
			objectAtt = Constants.PROJECT_INFO_OA_LBL;
		}

		return objectAtt;
	}
	

}
