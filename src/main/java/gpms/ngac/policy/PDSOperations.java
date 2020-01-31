package gpms.ngac.policy;

import gov.nist.csd.pm.epp.events.AssignToEvent;
import gov.nist.csd.pm.epp.events.DeassignFromEvent;
import gov.nist.csd.pm.epp.functions.FunctionExecutor;
import gov.nist.csd.pm.exceptions.PMException;
import gov.nist.csd.pm.pap.PAP;
import gov.nist.csd.pm.pdp.PDP;
import gov.nist.csd.pm.pdp.decider.Decider;
import gov.nist.csd.pm.pdp.decider.PReviewDecider;
import gov.nist.csd.pm.pip.graph.Graph;
import gov.nist.csd.pm.pip.graph.GraphSerializer;
import gov.nist.csd.pm.pip.graph.MemGraph;
import gov.nist.csd.pm.pip.graph.model.nodes.Node;
import gov.nist.csd.pm.pip.graph.model.nodes.NodeType;
import gov.nist.csd.pm.pip.obligations.MemObligations;
import gov.nist.csd.pm.pip.obligations.evr.EVRParser;
import gov.nist.csd.pm.pip.obligations.model.Obligation;
import gov.nist.csd.pm.pip.obligations.model.functions.Function;
import gov.nist.csd.pm.pip.prohibitions.MemProhibitions;
import gov.nist.csd.pm.pip.prohibitions.Prohibitions;
import gov.nist.csd.pm.pip.prohibitions.ProhibitionsSerializer;
import gov.nist.csd.pm.pip.prohibitions.model.Prohibition;
import gov.nist.csd.pm.pip.prohibitions.model.Prohibition.Subject.Type;
import gov.nist.csd.pm.pip.prohibitions.model.Prohibition.Subject;

import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.O;
import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.OA;
import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.U;
import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.UA;

import gpms.DAL.DepartmentsPositionsCollection;
import gpms.model.GPMSCommonInfo;
import gpms.rest.UserService;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
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
	
	//private GpmsNgacObligations  gpmsNgacObligations;
	
	public static HashMap<Long,Graph> proposalPolicies = new HashMap<Long,Graph>();
	public static Prohibitions proposalProhibitions = null;
	
	private static final Logger log = Logger.getLogger(PDSOperations.class.getName());
	
	private NGACPolicyConfigurationLoader policyLoader;
	
	
	
	Obligation obligation = null;	
	PDP pdp =null;
	
	public PDSOperations()
	{
		this.ngacPolicy = NGACPolicyConfigurationLoader.getPolicy();
		//gpmsNgacObligations = new GpmsNgacObligations();
		policyLoader = new NGACPolicyConfigurationLoader();	
		
	}
	
	public PDSOperations(Graph gf)
	{
		this.ngacPolicy = gf;
	//	gpmsNgacObligations = new GpmsNgacObligations();
	}
	
	public PDP getPDP(Graph graph) throws PMException {
		GetUserToDenySubjectExecutor getUserToDenySubjectExecuter = new GetUserToDenySubjectExecutor();

		if(pdp == null) {
			obligation = policyLoader.getObligation();
			pdp = new PDP(new PAP(graph, new MemProhibitions(), new MemObligations()),getUserToDenySubjectExecuter);
		 	pdp.getPAP().getObligationsPAP().add(obligation, true);	
		}
	 	return pdp;
	}

	public Graph getNGACPolicy() {
		return this.ngacPolicy;
	}
	
	public Graph getBacicNGACPolicy() {
		return policyLoader.reloadBasicConfig();
	}
	
	
	
	
	/**
	 * This function checks whether a user has permission for a task
	 * @param userName
	 * @return true/false
	 */
	public boolean hasPermissionToCreateAProposal( String userName, Prohibitions prohibitions)
	{
		boolean hasPermission = true;		
		
		HashMap map = Task.CREATE_PROPOSAL.getPermissionsSets();
		
		Iterator<Map.Entry<Attribute, HashSet>> itr = map.entrySet().iterator(); 
         
		while (itr.hasNext()) {
			Map.Entry<Attribute, HashSet> entry = itr.next();
			log.info("Container = " + entry.getKey() + ", permission set = " + entry.getValue());
			hasPermission = UserPermissionChecker.checkPermissionAnyType(ngacPolicy, prohibitions,
					userName, U.toString(), (Attribute) entry.getKey(),
					new ArrayList<String>(entry.getValue()));
		}
        
		log.info("Create Proposal Permission : "+hasPermission);
		
		return hasPermission;
	}
	
	
	public boolean hasPermissionToCreateAProposal(Graph policy, GPMSCommonInfo userInfo, Prohibitions prohibitions) {
		boolean hasPermission = true;

		HashMap map = Task.CREATE_PROPOSAL.getPermissionsSets();

		Iterator<Map.Entry<Attribute, HashSet>> itr = map.entrySet().iterator();

		while (itr.hasNext()) {
			Map.Entry<Attribute, HashSet> entry = itr.next();
			log.info("Container = " + entry.getKey() + ", permission set = " + entry.getValue());
			hasPermission = UserPermissionChecker.checkPermissionAnyType(policy, prohibitions, userInfo.getUserName(),
					U.toString(), (Attribute) entry.getKey(), new ArrayList<String>(entry.getValue()));
		}

		log.info("Create Proposal Permission : " + hasPermission);

		return hasPermission;
	}
	/**
	 * This function checks whether a user has permission to add another user as CoPI
	 * @param userName the performer
	 * @param coPIApproachableUser the intended user to be a CoPI
	 * @return true/false
	 */
	
	public boolean hasPermissionToAddAsCoPI(Graph policy, String userName,String coPIApproachableUser, Prohibitions prohibitions)
	{
		boolean hasPermission = true;		
		
		HashMap map = Task.ADD_CO_PI.getPermissionsSets();
		
		Iterator<Map.Entry<Attribute, HashSet>> itr = map.entrySet().iterator(); 
         
        while(itr.hasNext()) 
        { 
             Map.Entry<Attribute, HashSet> entry = itr.next(); 
             log.info("Container = " + entry.getKey() + ", permission set = " + entry.getValue()); 
             System.out.println("Container = " + entry.getKey() + ", permission set = " + entry.getValue()); 
             hasPermission = hasPermission && UserPermissionChecker.checkPermissionAnyType(policy, prohibitions, "PI", UA.toString(), (Attribute)entry.getKey(), new ArrayList<String>(entry.getValue()));
        } 
       // try {
       // 	hasPermission = hasPermission && isChildrenFound(policy, coPIApproachableUser, Constants.CO_PI_UA_LBL);
      //  }
      //  catch(PMException e){
       // 	e.printStackTrace();
       // }
		log.info("Add CoPI Permission : "+hasPermission);
		System.out.println("Add CoPI Permission : "+hasPermission);
		
		return hasPermission;
	}
	
	public boolean hasPermissionToDeleteCoPI(Graph policy, String userName, Prohibitions prohibitions)
	{
		boolean hasPermission = true;		
		
		HashMap map = Task.DELETE_CO_PI.getPermissionsSets();
		
		Iterator<Map.Entry<Attribute, HashSet>> itr = map.entrySet().iterator(); 
         
        while(itr.hasNext()) 
        { 
             Map.Entry<Attribute, HashSet> entry = itr.next(); 
             log.info("Container = " + entry.getKey() + ", permission set = " + entry.getValue()); 
             System.out.println("Container = " + entry.getKey() + ", permission set = " + entry.getValue()); 
             hasPermission = hasPermission && UserPermissionChecker.checkPermissionAnyType(policy, prohibitions, "PI", UA.toString(), (Attribute)entry.getKey(), new ArrayList<String>(entry.getValue()));
        } 
//        try {
//        	hasPermission = hasPermission && isChildrenFound(policy, coPIApproachableUser, Constants.CO_PI_UA_LBL);
//        }
//        catch(PMException e){
//        	e.printStackTrace();
//        }
		log.info("Delete CoPI Permission : "+hasPermission);
		System.out.println("Delete CoPI Permission : "+hasPermission);
		
		return hasPermission;
	}
	
	public boolean hasPermissionToDeleteSP(Graph policy, String userName, Prohibitions prohibitions)
	{
		boolean hasPermission = true;		
		
		HashMap map = Task.DELETE_SP.getPermissionsSets();
		
		Iterator<Map.Entry<Attribute, HashSet>> itr = map.entrySet().iterator(); 
         
        while(itr.hasNext()) 
        { 
             Map.Entry<Attribute, HashSet> entry = itr.next(); 
             log.info("Container = " + entry.getKey() + ", permission set = " + entry.getValue()); 
             System.out.println("Container = " + entry.getKey() + ", permission set = " + entry.getValue()); 
             hasPermission = hasPermission && UserPermissionChecker.checkPermissionAnyType(policy, prohibitions, "PI", UA.toString(), (Attribute)entry.getKey(), new ArrayList<String>(entry.getValue()));
        } 
//        try {
//        	hasPermission = hasPermission && isChildrenFound(policy, coPIApproachableUser, Constants.CO_PI_UA_LBL);
//        }
//        catch(PMException e){
//        	e.printStackTrace();
//        }
		log.info("Delete SP Permission : "+hasPermission);
		System.out.println("Delete SP Permission : "+hasPermission);
		
		return hasPermission;
	}
	
	
	/**
	 * This function checks whether a user has permission to add another user as SP
	 * @param userName the performer
	 * @param spApproachableUser the intended user to be a SP
	 * @return true/false
	 */
	
	public boolean hasPermissionToAddAsSP(Graph policy, String userName, String spApproachableUser, Prohibitions prohibitions) {
		boolean hasPermission = true;

		HashMap map = Task.ADD_SP.getPermissionsSets();

		Iterator<Map.Entry<Attribute, HashSet>> itr = map.entrySet().iterator();

		while (itr.hasNext()) {
			Map.Entry<Attribute, HashSet> entry = itr.next();
			log.info("Container = " + entry.getKey() + ", permission set = " + entry.getValue());
			hasPermission = hasPermission
					&& UserPermissionChecker.checkPermissionAnyType(policy, prohibitions, userName,
							U.toString(), (Attribute) entry.getKey(), new ArrayList<String>(entry.getValue()));
		}
//		try {
//			hasPermission = hasPermission
//					&& isChildrenFound(policy, spApproachableUser, Constants.SENIOR_PERSON_UA_LBL);
//		} catch (PMException e) {
//			e.printStackTrace();
//		}
		log.info("Add SP Permission : " + hasPermission);

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
	
	public long createAProposal(String userName) throws PMException {

		long randomId = getID();

		try {

			Graph proposalPolicy = null;
			// proposalPolicy = policyLoader.createAProposalGraph(ngacPolicy); //loads
			// editing policy
			proposalPolicy = policyLoader.reloadBasicConfig();
			proposalPolicy = policyLoader.createAProposalGraph(proposalPolicy); // loads editing policy
			proposalPolicy = policyLoader.createAprovalGraph(proposalPolicy); // loads editing policy

			Node pdsNode = proposalPolicy.createNode(randomId, "" + randomId, OA, null);
			//log.info("ID:" + randomId);
			long pdsOriginationOAID = getNodeID(proposalPolicy, Constants.PDS_ORIGINATING_OA, OA, null);
			long userID = getNodeID(proposalPolicy, userName, U, null);

			//printAccessState("Initial configuration before op:", proposalPolicy);
			log.info("CREATE PROPOSAL: # nodes BEFORE:"+proposalPolicy.getNodes().size());
			getPDP(proposalPolicy).getEPP().processEvent(
					new AssignToEvent(proposalPolicy.getNode(pdsOriginationOAID), pdsNode), userID, getID());
			
			
			/*
			long COPIUAID = getNodeID(proposalPolicy, Constants.CO_PI_UA_LBL, UA, null);
			long COPIUID = getNodeID(proposalPolicy, "liliana", U, null);
			
			
			getPDP(proposalPolicy).getEPP().processEvent(
					new AssignToEvent(proposalPolicy.getNode(COPIUAID), proposalPolicy.getNode(COPIUID)), userID, getID());
			
			*/
			
			
			log.info("Proposal policy saved:" + randomId + "|" + proposalPolicy.toString() + "|"
					+ proposalPolicy.getNodes().size());
			PDSOperations.proposalPolicies.put(randomId, proposalPolicy);
			//printAccessState("Initial configuration after op:", proposalPolicy);
			log.info("CREATE PROPOSAL: # nodes AFTER:"+proposalPolicy.getNodes().size());

		} catch (Exception e) {
			log.info("Exception:" + e.toString());
		}
		return randomId;
	}
	public Prohibitions submitAProposal(String userName) throws PMException {
		
		long randomId = getID();
		Prohibitions prohibitions = new MemProhibitions();

		try {
			Graph proposalPolicy = null;
			// proposalPolicy = policyLoader.createAProposalGraph(ngacPolicy); //loads
			// editing policy
			proposalPolicy = policyLoader.reloadBasicConfig();
			proposalPolicy = policyLoader.createAProposalGraph(proposalPolicy); // loads editing policy
			proposalPolicy = policyLoader.createAprovalGraph(proposalPolicy); // loads editing policy
			Node pdsNode = proposalPolicy.createNode(randomId, "" + randomId, OA, null);
			//log.info("ID:" + randomId);
			long pdsOriginationOAID = getNodeID(proposalPolicy, Constants.SUBMISSION_INFO_OA_LBL, OA, null);
			long userID = getNodeID(proposalPolicy, userName, U, null);

			//printAccessState("Initial configuration before op:", proposalPolicy);
			log.info("CREATE PROPOSAL: # nodes BEFORE:"+proposalPolicy.getNodes().size());
			PReviewDecider decider = new PReviewDecider(proposalPolicy);
			String[] array = new String[1];
			array[0] = "w";
			System.out.println("Before event: !!!!!!!!!!!!!!!!  "+decider.check(-4306211063214550717L, 101L, 375260122425903544L, array));

	   		 PDP pdp = getPDP(proposalPolicy);
	   		 try {
			pdp.getEPP().processEvent(
					new AssignToEvent(proposalPolicy.getNode(pdsOriginationOAID), pdsNode), userID, getID());
			
			
			
			prohibitions.add(pdp.getPAP().getProhibitionsPAP().get("deny1"));
	        System.out.println(pdp.getPAP().getProhibitionsPAP().get("deny1").getName());

	        prohibitions.add(pdp.getPAP().getProhibitionsPAP().get("deny2"));
	        System.out.println(pdp.getPAP().getProhibitionsPAP().get("deny2").getName());
	   		 }
	   		 catch(NullPointerException ex) {
	   			 ex.printStackTrace();
	   		 }

			log.info("SUBMIT PROPOSAL: # nodes AFTER:"+proposalPolicy.getNodes().size());

		} catch (Exception e) {
			log.info("Exception:" + e.toString());
		}
		return prohibitions;
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
    
    
    public static void printGraph(Graph graph) throws PMException {
    	List<Node> nodes =  (List<Node>) graph.getNodes();
    	System.out.println("***********Nodes:************");
    	for(Node node: nodes) {
    		System.out.println(node.getName());
    	}
    	
    }
	
	
	 /**
     * Method to simulate an obligation. All obligations used in this example are triggered by an "assign to" event,
     * so we'll assume there is a child node being assigned to a target node.
     * @param policy the graph
     * @param userID the ID of the user that triggered the event
     * @param targetNode the node that the event happens on
     * @throws PMException
     */
//    private void simulateAssignToEvent(Graph policy, long userID, Node targetNode, Node childNode) throws PMException {
//        // check if the target of the event is a particular container and execute the corresponding "response"
//        if(targetNode.getID() == getNodeID(policy, Constants.PDS_ORIGINATING_OA, OA, null)) {
//            //gpmsNgacObligations.createPDS(graph, userID, childNode);
//            gpmsNgacObligations.createPDS(policy, userID, childNode);
//        }
//        else if(targetNode.getID() == getNodeID(policy, "CoPI", OA, null)) {
//        	gpmsNgacObligations.addCoPI(policy, childNode);
//        } else if(targetNode.getID() == getNodeID(policy, "SP", OA, null)) {
//        	gpmsNgacObligations.addSP(policy, childNode);
//        } else if(targetNode.getID() == getNodeID(policy, "submitted_pdss", OA, null)) {
//        	gpmsNgacObligations.submitPDS(policy, childNode);
//        } else if(targetNode.getID() == getNodeID(policy, "cs_chair_approval", OA, null) ||
//                targetNode.getID() == getNodeID(policy, "math_chair_approval", OA, null)) {
//        	gpmsNgacObligations.chairApproval(policy, childNode);
//        }  else if(targetNode.getID() == getNodeID(policy, "coen_dean_approval", OA, null) ||
//                targetNode.getID() == getNodeID(policy, "coas_dean_approval", OA, null)) {
//        	gpmsNgacObligations.deanApproval(policy, childNode);
//        }
//    }
	
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
	
	public void testUsersAccessights_Proposal_created(Graph proposalPolicy) {
		try {
			long userIdNazmul = PDSOperations.getNodeID(proposalPolicy, "nazmul", NodeType.U, null);  //tanure track + cs
			long userIdAmy = PDSOperations.getNodeID(proposalPolicy, "amy", NodeType.U, null);     //adjunct
			long userIdtom = PDSOperations.getNodeID(proposalPolicy, "tomtom", NodeType.U, null);     //adjunct
			long userIdSamer = PDSOperations.getNodeID(proposalPolicy, "samer", NodeType.U, null);     // CE
			
			long userIdCSChair = PDSOperations.getNodeID(proposalPolicy, DepartmentsPositionsCollection.adminUsers.get("CSCHAIR"), NodeType.U, null);     
			
			log.info("************Start**************");
			Attribute att = new Attribute("Budget-Info", NodeType.OA);
			String[] ops = new String[] {"w"};
			boolean hasPermission = UserPermissionChecker.checkPermissionAnyType(proposalPolicy, new MemProhibitions(),"nazmul", "U",att, Arrays.asList(ops));
			log.info("Nazmul:Budget-Info(w):"+hasPermission);
			
			att = new Attribute("Budget-Info", NodeType.OA);
			ops = new String[] {"w"};
			String userName = "tomtom";
			hasPermission = UserPermissionChecker.checkPermissionAnyType(proposalPolicy, new MemProhibitions(),userName, "U",att, Arrays.asList(ops));
			log.info("tomtom:Budget-Info(w):"+hasPermission);
			
			att = new Attribute("Project-Info", NodeType.OA);
			ops = new String[] {"r"};
			userName = "tomtom";
			hasPermission = UserPermissionChecker.checkPermissionAnyType(proposalPolicy, new MemProhibitions(),userName, "U",att, Arrays.asList(ops));
			log.info("Project-Info(w):"+hasPermission);
			log.info("**************End************");
			
		}catch(PMException e) {
			e.printStackTrace();
		}
	}
	
	
	
	public void testUsersAccessights_Proposal_not_created() {
		try {
			log.info("************Start**************");
			long userIdNazmul = PDSOperations.getNodeID(ngacPolicy, "nazmul", NodeType.U, null);  //tanure track + cs
			long userIdAmy = PDSOperations.getNodeID(ngacPolicy, "amy", NodeType.U, null);     //adjunct
			Attribute att = new Attribute("PDS", NodeType.OA);
			String[] ops = new String[] {"create-oa"};
			boolean hasPermission = UserPermissionChecker.checkPermissionAnyType(ngacPolicy, new MemProhibitions(),"nazmul", "U",att, Arrays.asList(ops));
			log.info("Nazmul:Create Proposal:"+hasPermission);
			hasPermission = UserPermissionChecker.checkPermissionAnyType(ngacPolicy, new MemProhibitions(),"amy", "U",att, Arrays.asList(ops));
			log.info("Amy:Create Proposal:"+hasPermission);
			log.info("************End**************");
		}catch(PMException e) {
			e.printStackTrace();
		}
	}
    


}
