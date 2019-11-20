package gpms.ngac.policy;

import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.O;
import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.OA;
import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.U;
import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.UA;
import gpms.ngac.policy.ProposalStage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;

import gov.nist.csd.pm.exceptions.PMException;
import gov.nist.csd.pm.pip.graph.Graph;
import gov.nist.csd.pm.pip.graph.model.nodes.Node;
import gov.nist.csd.pm.pip.graph.model.nodes.NodeType;
import gpms.model.InvestigatorInfo;
import gpms.model.InvestigatorRefAndPosition;
import gpms.model.Proposal;

public class ProposalDataSheet {
	

	private static final Logger log = Logger.getLogger(ProposalDataSheet.class.getName());
	
	private Proposal proposalData;
	
	private Graph proposalPolicy;
	
	private ProposalStage proposalStage;
	
	public Proposal getProposal() {
		return proposalData;
	}

	public void setProposal(Proposal proposal) {
		this.proposalData = proposal;
	}

	public Graph getProposalPolicy() {
		return proposalPolicy;
	}

	public void setProposalPolicy(Graph proposalPolicy) {
		this.proposalPolicy = proposalPolicy;
	}
	
	public ProposalStage getProposalStage() {
		return proposalStage;
	}

	public void setProposalStage(ProposalStage proposalStage) {
		this.proposalStage = proposalStage;
	}

	public void updatePI() throws PMException{
		String userName = proposalData.getInvestigatorInfo().getPi().getUserRef().getUserAccount().getUserName();
		
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
	
	public void updateCoPI(String actor) throws PMException{
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
	
	public void updateSP(String actor) throws PMException{
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
	
	public String getPolicyDecision(String username,String action,String objectAtt) {
		
		
		log.info("userName:"+username+"| Action:"+action+"|OA:"+objectAtt);
		String decision ="";
		String[] operations = {action};
		ArrayList<String> ops = new ArrayList<String>();
		ops.addAll(Arrays.asList(operations));		
		Attribute att = new Attribute(objectAtt, NodeType.OA);
		boolean hasPermission = UserPermissionChecker.checkPermission2(proposalPolicy, username, att,  operations);
		if(hasPermission == true)
			decision = "Permit";
		else
			decision = "Deny";
		return decision;
	}
	
public String getPolicyDecisionAnyType(String subject,String type, String action,String objectAtt) {
		
		
		log.info("Subject:"+subject+"|type:"+type+" Action:"+action+"|OA:"+objectAtt);
		String decision ="";
		String[] operations = {action};
		ArrayList<String> ops = new ArrayList<String>();
		ops.addAll(Arrays.asList(operations));		
		Attribute att = new Attribute(objectAtt, NodeType.OA);
		boolean hasPermission = UserPermissionChecker.checkPermission(proposalPolicy, subject, type,att,  operations);
		if(hasPermission == true)
			decision = "Permit";
		else
			decision = "Deny";
		return decision;
	}
	
	
	public List<String> getPermittedActions(String username, String objectAtt) {
		
		ArrayList<String> permittedActions = new ArrayList<String>();
		List<String> ops = Arrays.asList("Save", "Submit",
				"Approve", "Disapprove", "Withdraw", "Archive", "Delete");
		
		//ArrayList<String> ops = new ArrayList<String>();
		//ops.addAll(Arrays.asList(operations));
		
		Attribute att = new Attribute(objectAtt, NodeType.OA);
		
		for(String operation : ops) {
			String[] operations = {operation};
			boolean hasPermission = UserPermissionChecker.checkPermission2(proposalPolicy, username, att,  operations);
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
		} else if (proposalSection.equalsIgnoreCase("Sponsor And Budget Information")) {
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
