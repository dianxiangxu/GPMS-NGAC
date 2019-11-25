package gpms.ngac.policy;

import gov.nist.csd.pm.exceptions.PMException;
import gov.nist.csd.pm.pip.graph.Graph;
import gov.nist.csd.pm.pip.graph.MemGraph;
import gov.nist.csd.pm.pip.graph.model.nodes.Node;
import gov.nist.csd.pm.pip.graph.model.nodes.NodeType;

import java.util.*;

import org.apache.log4j.Logger;

import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.O;
import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.OA;
import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.U;
import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.UA;
import static gpms.dev.PDS.getID;
import static gpms.dev.PDS.getNodeID;

/**
 * This class is intended to mimic the functionality of obligations through manipulation of a graph.
 * @author Md Nazmul Karim
 *
 */

public class GpmsNgacObligations {
	
	private static final Logger log = Logger.getLogger(GpmsNgacObligations.class.getName());

	//private PolicyCreationFromTemplate policyCreationFromTemplate;
	
	public GpmsNgacObligations() {
		//policyCreationFromTemplate = new PolicyCreationFromTemplate();
	}
	
	
	public void createPDS(Graph ngacPolicy, long userID, Node pdsRootNode) throws PMException {
		
		String userName = ngacPolicy.getNode(userID).getName();
		
		long id = pdsRootNode.getID();
		
		Graph proposalPolicy =null;
		proposalPolicy = new NGACPolicyConfigurationLoader().createAProposalGraph(proposalPolicy);
		
		Node proposalPI = proposalPolicy.createNode(getID(), userName, U, null);
        Node piName = proposalPolicy.createNode(getID(), userName, O, null);
        
        long piOANode = PDSOperations.getNodeID(proposalPolicy, Constants.PI_UA_LBL, OA, null);
        long piUANode = PDSOperations.getNodeID(proposalPolicy, Constants.PI_UA_LBL, UA, null);
        
        proposalPolicy.assign(proposalPI.getID(), piUANode);
        proposalPolicy.assign(piName.getID(), piOANode);
		
		log.info("Proposal policy saved:"+id +"|"+proposalPolicy.toString()+"|"+proposalPolicy.getNodes().size());
		PDSOperations.proposalPolicies.put(id, proposalPolicy);	
	
		
	}
	
	/*
	 * public void createPDSNew(Graph ngacPolicy, long userID, Node iPdsNode) throws
	 * PMException {
	 * 
	 * long id = iPdsNode.getID(); Node node =
	 * policyCreationFromTemplate.createPolicyFromTemplate(ngacPolicy, iPdsNode);
	 * Node userNode = ngacPolicy.getNode(userID);
	 * 
	 * 
	 * Node userObj = ngacPolicy.createNode(getID(), userNode.getName(), O, null);
	 * 
	 * // User Name is assigned to PI Object Attribute String piNodeLabel =
	 * Constants.PI_OA_UA_LBL+Long.toString(id); log.info("PI:"+piNodeLabel); long
	 * piOANodeId = getNodeID(ngacPolicy, piNodeLabel, OA, null);
	 * ngacPolicy.assign(userObj.getID(), piOANodeId);
	 * 
	 * //User is assigned to PI user Attribute long piUANodeId =
	 * getNodeID(ngacPolicy, piNodeLabel, UA, null);
	 * ngacPolicy.assign(userNode.getID(), piUANodeId);
	 * 
	 * //iPDS node is assigned to PDSs long pdssNode = getNodeID(ngacPolicy,
	 * Constants.PDSs_OA_UA_LBL, OA, null); //PDSs OA
	 * ngacPolicy.assign(node.getID(), pdssNode);
	 * 
	 * }
	 */
	
    /**
     * Obligation to create a new PDS, the given user is the PI, the given node is the base node for the PDS.
     * Some things are hard-coded.
     * @param ngacPolicy
     * @param userID
     * @param pdsNode
     * @throws PMException
     */
	/*
	 * public static void createPDS(Graph ngacPolicy, long userID, Node iPdsNode)
	 * throws PMException { // get the node for the user Node userNode =
	 * ngacPolicy.getNode(userID);
	 * 
	 * long id = iPdsNode.getID();
	 * 
	 * log.info("New PDS ID:"+id);
	 * 
	 * //createInvertigatorUserAttributes(ngacPolicy, iPdsNode, userNode, id);
	 * 
	 * // create the object attribute hierarchy
	 * 
	 * // create the investigator info container and assign it to the PDS Node
	 * investigatorInfoNode = ngacPolicy.createNode(getID(),
	 * Constants.INVESTIGATOR_OA_UA_LBL+id, OA, null);
	 * ngacPolicy.assign(investigatorInfoNode.getID(), iPdsNode.getID());
	 * 
	 * // create the PI container and assign it to InvestigatorInfo Node piNode =
	 * ngacPolicy.createNode(getID(), Constants.PI_OA_UA_LBL+id, OA, null);
	 * ngacPolicy.assign(piNode.getID(), investigatorInfoNode.getID());
	 * 
	 * // create a node with the name of the user and assign it to the PI container
	 * Node userObj = ngacPolicy.createNode(getID(), userNode.getName(), O, null);
	 * ngacPolicy.assign(userObj.getID(), piNode.getID());
	 * 
	 * // create the CoPI container and assign it to InvestigatorInfo Node copiNode
	 * = ngacPolicy.createNode(getID(), Constants.CO_PI_OA_UA_LBL+id, OA, null);
	 * ngacPolicy.assign(copiNode.getID(), investigatorInfoNode.getID());
	 * 
	 * // create the SP container and assign it to InvestigatorInfo Node spNode =
	 * ngacPolicy.createNode(getID(), Constants.SENIOR_PERSON_OA_UA_LBL+id, OA,
	 * null); ngacPolicy.assign(spNode.getID(), investigatorInfoNode.getID());
	 * 
	 * // create the ProjectInfo container and assign it to the PDS Node
	 * projInfoNode = ngacPolicy.createNode(getID(),
	 * Constants.PROJECT_INFO_OA_LBL+id, OA, null);
	 * ngacPolicy.assign(projInfoNode.getID(), iPdsNode.getID());
	 * 
	 * // create the BudgetInfo container and assign it to the PDS Node budgInfoNode
	 * = ngacPolicy.createNode(getID(), Constants.BUDGET_INFO_OA_LBL+id, OA, null);
	 * ngacPolicy.assign(budgInfoNode.getID(), iPdsNode.getID());
	 * 
	 * long pdssNode = getNodeID(ngacPolicy, Constants.PDSs_OA_UA_LBL, OA, null);
	 * //PDSs OA ngacPolicy.assign(iPdsNode.getID(), pdssNode);
	 * 
	 * // now create user attribute hierarchy
	 * 
	 * // create a base user attribute for the PDS, assign it to a user attribute in
	 * the PDS policy class long pdssUA = getNodeID(ngacPolicy,
	 * Constants.PDSs_OA_UA_LBL, UA, null); //PDSs
	 * 
	 * Node iPdsUA = ngacPolicy.createNode(getID(), iPdsNode.getName()+id, UA,
	 * null); ngacPolicy.assign(iPdsUA.getID(), pdssUA);
	 * 
	 * // create a user attribute for the PI and assign it to the PDSi base user
	 * attribute Node piUA = ngacPolicy.createNode(getID(),
	 * Constants.PI_OA_UA_LBL+id, UA, null); ngacPolicy.assign(piUA.getID(),
	 * iPdsUA.getID());
	 * 
	 * // create a user attribute for the CoPI and assign it to the PDSi base user
	 * attribute Node copiUA = ngacPolicy.createNode(getID(),
	 * Constants.CO_PI_OA_UA_LBL+id, UA, null); ngacPolicy.assign(copiUA.getID(),
	 * iPdsUA.getID());
	 * 
	 * // create a user attribute for the SP and assign it to the PDSi base user
	 * attribute Node spUA = ngacPolicy.createNode(getID(),
	 * Constants.SENIOR_PERSON_OA_UA_LBL+id, UA, null);
	 * ngacPolicy.assign(spUA.getID(), iPdsUA.getID());
	 * 
	 * // assign the current user to the PI user attribute
	 * ngacPolicy.assign(userNode.getID(), piUA.getID());
	 * 
	 * // grant PI permissions on the CoPI container Long org_PDSsOAid =
	 * PDSOperations.getNodeID(ngacPolicy, Constants.PDS_ORIGINATING_OA, OA,
	 * null);//graph.createNode(getID(), Constants.FACULTY_UA_LBL, UA, null);
	 * 
	 * associatePIRights(ngacPolicy, piUA, copiNode, org_PDSsOAid, copiUA,
	 * projInfoNode, budgInfoNode, iPdsNode, investigatorInfoNode);
	 * 
	 * associateCoPIRights(ngacPolicy, copiUA, spNode , projInfoNode, budgInfoNode);
	 * 
	 * // grant SP permissions on the InvestigatorInfo container
	 * associateSPRights(ngacPolicy, spUA, investigatorInfoNode, projInfoNode,
	 * budgInfoNode);
	 * 
	 * }
	 */
	 
//	private static void associatePIRights(Graph ngacPolicy, Node piUA, Node copiNode,  long org_PDSsOAid, Node copiUA,
//			Node projInfoNode, Node budgInfoNode, Node iPdsNode, Node investigatorInfoNode) throws PMException {
//		
//		ngacPolicy.associate(piUA.getID(), copiNode.getID(), new HashSet<>(Arrays.asList("read", "create-o","create-o-to-oa")));
//		ngacPolicy.associate(piUA.getID(), org_PDSsOAid, new HashSet<>(Arrays.asList("assign from")));
//		ngacPolicy.associate(piUA.getID(), copiUA.getID(), new HashSet<>(Arrays.asList("assign-u to")));
//
//		// grant PI permissions on the ProjectInfo container
//		ngacPolicy.associate(piUA.getID(), projInfoNode.getID(),
//				new HashSet<>(Arrays.asList("write", "read", "create-o", "create-o-to-oa")));
//		// grant PI permissions on the BudgetInfo container
//		ngacPolicy.associate(piUA.getID(), budgInfoNode.getID(),
//				new HashSet<>(Arrays.asList("write", "read", "create-o", "create-o-to-oa")));
//		ngacPolicy.associate(piUA.getID(), budgInfoNode.getID(),
//				new HashSet<>(Arrays.asList("read", "create-o", "create-o-to-oa")));
//		// grant PI permissions to assign the PDS
//		ngacPolicy.associate(piUA.getID(), iPdsNode.getID(),
//				new HashSet<>(Arrays.asList("create-oa", "create-oa-to-oa")));
//		// grant PI permission to read the PI container
//		ngacPolicy.associate(piUA.getID(), investigatorInfoNode.getID(), new HashSet<>(Arrays.asList("read")));
//
//	}
//	 
//	private static void associateCoPIRights(Graph ngacPolicy, Node copiUA, Node spNode, Node projInfoNode,
//			Node budgInfoNode) throws PMException {
//		// grant CoPI permissions on the PDS
//		// grant CoPI permissions on the SP container
//		ngacPolicy.associate(copiUA.getID(), spNode.getID(),
//				new HashSet<>(Arrays.asList("read", "create-o", "create-o-to-oa")));
//		// grant CoPI permissions on the ProjectInfo container
//		ngacPolicy.associate(copiUA.getID(), projInfoNode.getID(),
//				new HashSet<>(Arrays.asList("write", "read", "create-o", "create-o-to-oa")));
//		// grant CoPI permissions on the BudgetInfo container
//		ngacPolicy.associate(copiUA.getID(), budgInfoNode.getID(), new HashSet<>(Arrays.asList("read")));
//
//	}
//	
//	private static void associateSPRights(Graph ngacPolicy, Node spUA, Node investigatorInfoNode, Node projInfoNode,
//			Node budgInfoNode) throws PMException {
//		ngacPolicy.associate(spUA.getID(), investigatorInfoNode.getID(), new HashSet<>(Arrays.asList("read")));
//		// grant SP permissions on the ProjectInfo container
//		ngacPolicy.associate(spUA.getID(), projInfoNode.getID(), new HashSet<>(Arrays.asList("read")));
//		// grant SP permissions on the BudgetInfo container
//		ngacPolicy.associate(spUA.getID(), budgInfoNode.getID(), new HashSet<>(Arrays.asList("read")));
//	}
//	 private static void createInvertigatorUserAttributes(Graph ngacPolicy,Node pdsNode, Node userNode,long id) {
//		 
//	 }

    /**
     * When an object is added to the CoPI container get the user with the same name and assign that user to the CoPI user attribute.
     */
    public static void addCoPI(Graph graph, Node copiNode) throws PMException {
        long user = PDSOperations.getNodeID(graph, copiNode.getName(), U, null);
        long ua = PDSOperations.getNodeID(graph, "CoPI", UA, null);

        graph.assign(user, ua);
    }

    /**
     * When an object is added to the SP container get the user with the same name and assign that user to the SP user attribute.
     */
    public static void addSP(Graph graph, Node spNode) throws PMException {
        long user = PDSOperations.getNodeID(graph, spNode.getName(), U, null);
        long ua = PDSOperations.getNodeID(graph, "SP", UA, null);

        graph.assign(user, ua);
    }

    public static void submitPDS(Graph graph, Node pdsNode) throws PMException {
        // deassign the PDS from the RBAC and PDS policy classes
        graph.deassign(pdsNode.getID(), PDSOperations.getNodeID(graph, "org_PDSs", OA, null));
        graph.deassign(pdsNode.getID(), PDSOperations.getNodeID(graph, "PDSs", OA, null));

        // delete the associations for the PI
        long piID = PDSOperations.getNodeID(graph, "PI", UA, null);
        graph.dissociate(piID, PDSOperations.getNodeID(graph, "CoPI", OA, null));
        graph.dissociate(piID, PDSOperations.getNodeID(graph, "ProjectInfo", OA, null));
        graph.dissociate(piID, PDSOperations.getNodeID(graph, "BudgetInfo", OA, null));
        graph.dissociate(piID, PDSOperations.getNodeID(graph, "PDSi", OA, null));
        graph.dissociate(piID, PDSOperations.getNodeID(graph, "PI", OA, null));

        long copiID = PDSOperations.getNodeID(graph, "CoPI", UA, null);
        graph.dissociate(copiID, PDSOperations.getNodeID(graph, "SP", OA, null));
        graph.dissociate(copiID, PDSOperations.getNodeID(graph, "ProjectInfo", OA, null));
        graph.dissociate(copiID, PDSOperations.getNodeID(graph, "BudgetInfo", OA, null));

        long spID = PDSOperations.getNodeID(graph, "SP", UA, null);
        graph.dissociate(spID, PDSOperations.getNodeID(graph, "InvestigatorInfo", OA, null));
        graph.dissociate(spID, PDSOperations.getNodeID(graph, "ProjectInfo", OA, null));
        graph.dissociate(spID, PDSOperations.getNodeID(graph, "BudgetInfo", OA, null));

        // remove the pds from the submitted_pdss container, so no rbac permissions are given
        graph.deassign(pdsNode.getID(), PDSOperations.getNodeID(graph, "submitted_pdss", OA, null));

        Node pdsiApprovalOA = graph.createNode(getID(), "PDSi approval", OA, null);
        graph.assign(pdsiApprovalOA.getID(), PDSOperations.getNodeID(graph,"approval", OA, null));

        Node pendingOA = graph.createNode(getID(), "PDSi approval", OA, null);
        graph.assign(pendingOA.getID(), pdsiApprovalOA.getID());

        Node approvedOA = graph.createNode(getID(), "approved", OA, null);
        graph.assign(approvedOA.getID(), pdsiApprovalOA.getID());

        Node chairApprovalOA = graph.createNode(getID(), "chair approval", OA, null);
        graph.assign(chairApprovalOA.getID(), pendingOA.getID());

        Node deanApprovalOA = graph.createNode(getID(), "dean approval", OA, null);
        graph.assign(deanApprovalOA.getID(), pendingOA.getID());

        graph.assign(pdsNode.getID(), chairApprovalOA.getID());

        Node csChairApproval = graph.createNode(getID(), "cs_chair_approval", OA, null);
        graph.assign(csChairApproval.getID(), approvedOA.getID());

        Node mathChairApproval = graph.createNode(getID(), "math_chair_approval", OA, null);
        graph.assign(mathChairApproval.getID(), approvedOA.getID());

        Node coenDeanApproval = graph.createNode(getID(), "coen_dean_approval", OA, null);
        graph.assign(coenDeanApproval.getID(), approvedOA.getID());

        Node coasDeanApproval = graph.createNode(getID(), "coas_dean_approval", OA, null);
        graph.assign(coasDeanApproval.getID(), approvedOA.getID());

        // create the user attribute structure for approvals
        Node pdsiApprovalUA = graph.createNode(getID(), "PDSi approval", UA, null);
        graph.assign(pdsiApprovalUA.getID(), PDSOperations.getNodeID(graph, "approval", UA, null));

        Node pdsiCommitteeUA = graph.createNode(getID(), "PDSi committee", UA, null);
        graph.assign(pdsiCommitteeUA.getID(), PDSOperations.getNodeID(graph, "PDSi approval", UA, null));

        graph.associate(pdsiCommitteeUA.getID(), pdsiApprovalOA.getID(), new HashSet<>(Arrays.asList("read")));

        // assign all users of PI CoPI and SP to the PDSi committee
        Set<Long> users = graph.getChildren(PDSOperations.getNodeID(graph, "PI", UA, null));
        for(Long id : users) {
            graph.assign(id, pdsiCommitteeUA.getID());
        }

        users = graph.getChildren(PDSOperations.getNodeID(graph, "CoPI", UA, null));
        for(Long id : users) {
            graph.assign(id, pdsiCommitteeUA.getID());
        }

        users = graph.getChildren(PDSOperations.getNodeID(graph, "SP", UA, null));
        for(Long id : users) {
            graph.assign(id, pdsiCommitteeUA.getID());
        }

        graph.associate(pdsiCommitteeUA.getID(), pdsiApprovalOA.getID(), new HashSet<>(Arrays.asList("read")));

        Node approvalCommitteeUA = graph.createNode(getID(), "approval committee", UA, null);
        graph.assign(approvalCommitteeUA.getID(), pdsiApprovalUA.getID());

        Node chairsUA = graph.createNode(getID(), "chairs", UA, null);
        graph.assign(chairsUA.getID(), PDSOperations.getNodeID(graph, "approval committee", UA, null));

        Node deansUA = graph.createNode(getID(), "deans", UA, null);
        graph.assign(deansUA.getID(), PDSOperations.getNodeID(graph, "approval committee", UA, null));

        graph.associate(chairsUA.getID(), chairApprovalOA.getID(), new HashSet<>(Arrays.asList("assign", "read")));
        graph.associate(deansUA.getID(), deanApprovalOA.getID(), new HashSet<>(Arrays.asList("assign", "read")));

        graph.associate(PDSOperations.getNodeID(graph, "CS_Chair", UA, null), csChairApproval.getID(), new HashSet<>(Arrays.asList("assign to")));
        graph.associate(PDSOperations.getNodeID(graph, "Math_Chair", UA, null), mathChairApproval.getID(), new HashSet<>(Arrays.asList("assign to")));
        graph.associate(PDSOperations.getNodeID(graph, "COEN_Dean", UA, null), coenDeanApproval.getID(), new HashSet<>(Arrays.asList("assign to")));
        graph.associate(PDSOperations.getNodeID(graph, "COAS_Dean", UA, null), coasDeanApproval.getID(), new HashSet<>(Arrays.asList("assign to")));

        graph.assign(PDSOperations.getNodeID(graph, "CS_Chair", U, null), chairsUA.getID());
        graph.assign(PDSOperations.getNodeID(graph, "Math_Chair", U, null), chairsUA.getID());

        graph.assign(PDSOperations.getNodeID(graph, "COEN_Dean", U, null), deansUA.getID());
        graph.assign(PDSOperations.getNodeID(graph, "COAS_Dean", U, null), deansUA.getID());
    }

    public static void chairApproval(Graph graph, Node pdsNode) throws PMException {
        // check that the PDS node is assigned to the cs chair approval and math chair approval container
        if(!graph.getParents(pdsNode.getID()).contains(PDSOperations.getNodeID(graph, "cs_chair_approval", OA, null)) ||
                !graph.getParents(pdsNode.getID()).contains(PDSOperations.getNodeID(graph, "math_chair_approval", OA, null))) {
            return;
        }

        // remove the PDS from the chair approval containers
        graph.deassign(pdsNode.getID(), PDSOperations.getNodeID(graph, "chair approval", OA, null));
        graph.deassign(pdsNode.getID(), PDSOperations.getNodeID(graph, "cs_chair_approval", OA, null));
        graph.deassign(pdsNode.getID(), PDSOperations.getNodeID(graph, "math_chair_approval", OA, null));

        // assign the PDS to the dean approval container
        graph.assign(pdsNode.getID(), PDSOperations.getNodeID(graph, "dean approval", OA, null));

        // grant the chairs read permissions on the PDS
        graph.associate(PDSOperations.getNodeID(graph, "chairs", UA, null), pdsNode.getID(), new HashSet<>(Arrays.asList("read")));
    }

    public static void deanApproval(Graph graph, Node pdsNode) throws PMException {
        // check that the PDS node is assigned to the coen dean approval and coas dean approval conatiner
        if(!graph.getParents(pdsNode.getID()).contains(PDSOperations.getNodeID(graph, "coas_dean_approval", OA, null)) ||
                !graph.getParents(pdsNode.getID()).contains(PDSOperations.getNodeID(graph, "coen_dean_approval", OA, null))) {
            return;
        }

        // remove the PDS from the dean approval containers
        graph.deassign(pdsNode.getID(), PDSOperations.getNodeID(graph, "dean approval", OA, null));
        graph.deassign(pdsNode.getID(), PDSOperations.getNodeID(graph, "coen_dean_approval", OA, null));
        graph.deassign(pdsNode.getID(), PDSOperations.getNodeID(graph, "coas_dean_approval", OA, null));

        // assign the PDS to the dean approval container
        graph.assign(pdsNode.getID(), PDSOperations.getNodeID(graph, "PDSi approval", OA, null));

        // grant the deans read permissions on the PDS
        graph.associate(PDSOperations.getNodeID(graph, "deans", UA, null), pdsNode.getID(), new HashSet<>(Arrays.asList("read")));
    }

}
