package gpms.ngac.policy;

import gov.nist.csd.pm.epp.events.AssignToEvent;
import gov.nist.csd.pm.exceptions.PMException;
import gov.nist.csd.pm.pap.PAP;
import gov.nist.csd.pm.pdp.PDP;
import gov.nist.csd.pm.pdp.decider.Decider;
import gov.nist.csd.pm.pdp.decider.PReviewDecider;
import gov.nist.csd.pm.pip.graph.Graph;
import gov.nist.csd.pm.pip.graph.MemGraph;
import gov.nist.csd.pm.pip.graph.model.nodes.Node;
import gov.nist.csd.pm.pip.graph.model.nodes.NodeType;
import gov.nist.csd.pm.pip.obligations.MemObligations;
import gov.nist.csd.pm.pip.obligations.evr.EVRParser;
import gov.nist.csd.pm.pip.obligations.model.Obligation;
import gov.nist.csd.pm.pip.prohibitions.MemProhibitions;

import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.O;
import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.OA;
import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.U;
import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.UA;
import gpms.model.GPMSCommonInfo;
import gpms.rest.UserService;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;




/**
 * @author Md Nazmul Karim
 * @since May 20 2019
 * 
 * This class is used to operate different functions over NGAC policy
 */
public class PDSOperations {
	
	private Graph ngacPolicy;
	public static Random rand = new Random();
	
	private GpmsNgacObligations  gpmsNgacObligations;
	
	public static HashMap<Long,Graph> proposalPolicies = new HashMap<Long,Graph>();
	
	private static final Logger log = Logger.getLogger(PDSOperations.class.getName());
	
	private NGACPolicyConfigurationLoader policyLoader;
	
	public PDSOperations()
	{
		this.ngacPolicy = NGACPolicyConfigurationLoader.getPolicy();
		gpmsNgacObligations = new GpmsNgacObligations();
		policyLoader = new NGACPolicyConfigurationLoader();
		
	}
	
	public PDSOperations(Graph gf)
	{
		this.ngacPolicy = gf;
		gpmsNgacObligations = new GpmsNgacObligations();
	}
	
	public Graph getNGACPolicy() {
		return this.ngacPolicy;
	}
	
	public Graph getBacicNGACPolicy() {
		return policyLoader.reloadBasicConfig();
	}
	
	
	/**
	 * This function checks whether a user has permission for a task
	 * @param userInfo
	 * @return true/false
	 */
	public boolean hasPermissionToCreateAProposal( GPMSCommonInfo userInfo)
	{
		boolean hasPermission = true;		
		
		HashMap map = Task.CREATE_PROPOSAL.getPermissionsSets();
		
		Iterator<Map.Entry<Attribute, HashSet>> itr = map.entrySet().iterator(); 
         
        while(itr.hasNext()) 
        { 
             Map.Entry<Attribute, HashSet> entry = itr.next(); 
             log.info("Container = " + entry.getKey() +  
                                 ", permission set = " + entry.getValue()); 
             hasPermission = UserPermissionChecker.checkPermission(ngacPolicy, userInfo.getUserName(),U.toString(), (Attribute)entry.getKey(), entry.getValue().toArray());
        } 
        
		log.info("Create Proposal Permission : "+hasPermission);
		
		return hasPermission;
	}
	
	
	public boolean hasPermissionToCreateAProposal(Graph policy, GPMSCommonInfo userInfo)
	{
		boolean hasPermission = true;		
		
		HashMap map = Task.CREATE_PROPOSAL.getPermissionsSets();
		
		Iterator<Map.Entry<Attribute, HashSet>> itr = map.entrySet().iterator(); 
         
        while(itr.hasNext()) 
        { 
             Map.Entry<Attribute, HashSet> entry = itr.next(); 
             log.info("Container = " + entry.getKey() +  
                                 ", permission set = " + entry.getValue()); 
             hasPermission = UserPermissionChecker.checkPermission(policy, userInfo.getUserName(),U.toString(), (Attribute)entry.getKey(), entry.getValue().toArray());
        } 
        
		log.info("Create Proposal Permission : "+hasPermission);
		
		return hasPermission;
	}
	/**
	 * This function checks whether a user has permission to add another user as CoPI
	 * @param userInfo the performer
	 * @param coPIApproachableUser the intended user to be a CoPI
	 * @return true/false
	 */
	
	public boolean hasPermissionToAddAsCoPI(Graph policy, GPMSCommonInfo userInfo,String coPIApproachableUser)
	{
		boolean hasPermission = true;		
		
		HashMap map = Task.ADD_CO_PI.getPermissionsSets();
		
		Iterator<Map.Entry<Attribute, HashSet>> itr = map.entrySet().iterator(); 
         
        while(itr.hasNext()) 
        { 
             Map.Entry<Attribute, HashSet> entry = itr.next(); 
             log.info("Container = " + entry.getKey() + ", permission set = " + entry.getValue()); 
             System.out.println("Container = " + entry.getKey() + ", permission set = " + entry.getValue()); 
             hasPermission = hasPermission && UserPermissionChecker.checkPermission(policy, "PI", UA.toString(), (Attribute)entry.getKey(), entry.getValue().toArray());
        } 
        try {
        	hasPermission = hasPermission && isChildrenFound(policy, coPIApproachableUser, Constants.CO_PI_UA_LBL);
        }
        catch(PMException e){
        	e.printStackTrace();
        }
		log.info("Add CoPI Permission : "+hasPermission);
		System.out.println("Add CoPI Permission : "+hasPermission);
		
		return hasPermission;
	}
	
	
	/**
	 * This function checks whether a user has permission to add another user as SP
	 * @param userInfo the performer
	 * @param spApproachableUser the intended user to be a SP
	 * @return true/false
	 */
	
	public boolean hasPermissionToAddAsSP(Graph policy,GPMSCommonInfo userInfo,String spApproachableUser)
	{
		boolean hasPermission = true;		
		
		HashMap map = Task.ADD_SP.getPermissionsSets();
		
		Iterator<Map.Entry<Attribute, HashSet>> itr = map.entrySet().iterator(); 
         
        while(itr.hasNext()) 
        { 
             Map.Entry<Attribute, HashSet> entry = itr.next(); 
             log.info("Container = " + entry.getKey() +  
                                 ", permission set = " + entry.getValue()); 
            // hasPermission = hasPermission && UserPermissionChecker.checkPermission(policy, userInfo.getUserName(), U.toString(), (Attribute)entry.getKey(), entry.getValue().toArray());
        } 
        try {
        	hasPermission = hasPermission && isChildrenFound(policy, spApproachableUser, Constants.SENIOR_PERSON_UA_LBL);
        }
        catch(PMException e){
        	e.printStackTrace();
        }
		log.info("Add SP Permission : "+hasPermission);
		
		return hasPermission;
	}
	
	
	
	/*
	 * public long createAProposal(String userName) throws PMException {
	 * printAccessState("Before User creates PDS", ngacPolicy); long randomId =
	 * getID(); //user creates a PDS and assigns it to Constants.PDS_ORIGINATING_OA
	 * Node pdsNode = this.ngacPolicy.createNode(randomId,
	 * createProposalId(randomId), OA, null);
	 * 
	 * long pdsOriginationOAID = getNodeID(ngacPolicy, Constants.PDS_ORIGINATING_OA,
	 * OA, null); ngacPolicy.assign(pdsNode.getID(), pdsOriginationOAID);
	 * 
	 * 
	 * long userID = getNodeID(ngacPolicy, userName, U, null);
	 * simulateAssignToEvent(ngacPolicy, userID,
	 * ngacPolicy.getNode(pdsOriginationOAID), pdsNode);
	 * 
	 * printAccessState("After User creates PDS", ngacPolicy);
	 * 
	 * return randomId; }
	 */
	
	public long createAProposal(String userName) throws PMException
	{
		
		long randomId = getID(); 
		Node pdsNode = this.ngacPolicy.createNode(randomId, ""+randomId, OA, null);
		log.info("ID:"+randomId);
		
		long pdsOriginationOAID = getNodeID(ngacPolicy, Constants.PDS_ORIGINATING_OA,  OA, null); ngacPolicy.assign(pdsNode.getID(), pdsOriginationOAID);
		//ngacPolicy.assign(pdsNode.getID(), pdsOriginationOAID);
		try {
		long userID = getNodeID(ngacPolicy, userName, U, null);
		//simulateAssignToEvent(ngacPolicy, userID, ngacPolicy.getNode(pdsOriginationOAID), pdsNode);
		
		//added
		
		Graph proposalPolicy =null;
		proposalPolicy = policyLoader.createAProposalGraph(ngacPolicy); //loads editing policy
		printAccessState("Initial configuration before op:", proposalPolicy);
		
		Obligation obligation = null;		
		obligation = policyLoader.loadObligation(Constants.OBLIGATION_TEMPLATE_PROPOSAL_CREATION);
		
		PDP pdp = new PDP(new PAP(proposalPolicy, new MemProhibitions(), new MemObligations()));
	 	pdp.getPAP().getObligationsPAP().add(obligation, true);	  
	    pdp.getEPP().processEvent(new AssignToEvent(proposalPolicy.getNode(pdsOriginationOAID), pdsNode),userID, getID());

		
	    log.info("Proposal policy saved:"+randomId +"|"+proposalPolicy.toString()+"|"+proposalPolicy.getNodes().size());
		PDSOperations.proposalPolicies.put(randomId, proposalPolicy);	
		printAccessState("Initial configuration after op:", proposalPolicy);

			
		}catch(Exception e) {
			log.info("Exception:"+ e.toString() );
		}
        return randomId;
	}
	
	
	private String createProposalId(long id)
	{
		return "PDS"+id;
	}
	
	
	 /**
     * Utility method to print the current access state to the console.
     * @param step the name of the step
     * @param graph the graph to determine permissions
     */
    public static void printAccessState(String step, Graph graph) throws PMException {
        System.out.println("############### Access state for " + step + " ###############");

        // initialize a PReviewDecider to make decisions
        PReviewDecider decider = new PReviewDecider(graph);

        // get all of the users in the graph
        Set<Node> search = graph.search(null, U.toString(), null);
        for(Node user : search) {
            // there is a super user that we'll ignore
            if(user.getName().equals("super")) {
                continue;
            }

            log.info(user.getName());
            // get all of the nodes accessible for the current user
            Map<Long, Set<String>> accessibleNodes = decider.getAccessibleNodes(user.getID(),100);
            for(long objectID : accessibleNodes.keySet()) {
                Node obj = graph.getNode(objectID);
                log.info("\t" + obj.getName() + " -> " + accessibleNodes.get(objectID));
            }
        }
        log.info("############### End Access state for " + step + "############");
    }
	
	
	 /**
     * Method to simulate an obligation. All obligations used in this example are triggered by an "assign to" event,
     * so we'll assume there is a child node being assigned to a target node.
     * @param policy the graph
     * @param userID the ID of the user that triggered the event
     * @param targetNode the node that the event happens on
     * @throws PMException
     */
    private void simulateAssignToEvent(Graph policy, long userID, Node targetNode, Node childNode) throws PMException {
        // check if the target of the event is a particular container and execute the corresponding "response"
        if(targetNode.getID() == getNodeID(policy, Constants.PDS_ORIGINATING_OA, OA, null)) {
            //gpmsNgacObligations.createPDS(graph, userID, childNode);
            gpmsNgacObligations.createPDS(policy, userID, childNode);
        }
        else if(targetNode.getID() == getNodeID(policy, "CoPI", OA, null)) {
        	gpmsNgacObligations.addCoPI(policy, childNode);
        } else if(targetNode.getID() == getNodeID(policy, "SP", OA, null)) {
        	gpmsNgacObligations.addSP(policy, childNode);
        } else if(targetNode.getID() == getNodeID(policy, "submitted_pdss", OA, null)) {
        	gpmsNgacObligations.submitPDS(policy, childNode);
        } else if(targetNode.getID() == getNodeID(policy, "cs_chair_approval", OA, null) ||
                targetNode.getID() == getNodeID(policy, "math_chair_approval", OA, null)) {
        	gpmsNgacObligations.chairApproval(policy, childNode);
        }  else if(targetNode.getID() == getNodeID(policy, "coen_dean_approval", OA, null) ||
                targetNode.getID() == getNodeID(policy, "coas_dean_approval", OA, null)) {
        	gpmsNgacObligations.deanApproval(policy, childNode);
        }
    }
	
	public static long getNodeID(Graph graph, String name, NodeType type, Map<String, String> properties) throws PMException {
        Set<Node> search = graph.search(name, type.toString(), properties);
        if(search.isEmpty()) {
            throw new PMException("no node with name " + name + ", type " + type + ", and properties " + properties);
        }

        return search.iterator().next().getID();
    }
	
	public static long getID() {
        return rand.nextLong();
    }
	
	public boolean doesPolicyBelongToNGAC(HashMap<String,String> attr)
	{
		if(attr.get("position.type") != null && 
				attr.get("proposal.section").equalsIgnoreCase("Whole Proposal") &&
				attr.get("proposal.action").equalsIgnoreCase("Add"))
			return true;
		return false;
	}
	
	public boolean isChildrenFound(Graph policy, String name,String parent) throws PMException
    {
    	boolean found = false;
        // get all of the users in the graph
        Set<Node> search = policy.search(parent, UA.toString(), null);
        
        System.out.println(search.size());
        
        for(Node userAttNode : search) {
        	
        	 Set<Long> childIds = policy.getChildren(userAttNode.getID());
        	 log.info("No of Children Assigned on "+parent+" :"+childIds.size()+"|"+childIds);
        	 
        	 long sourceNode = getNodeID(policy, name, U, null);
        	 
        	 log.info("We are looking for:"+sourceNode);
        	 
        	 if(childIds.contains(sourceNode))
        	 {	
        		 found = true;
        		 log.info("found");
        	 }
        	 else
        	 {
        		 log.info("not found");
        	 }
        }
        return found;
        
    }
	
    


}