package gpms.ngac.policy;

import gov.nist.csd.pm.decider.PReviewDecider;
import gov.nist.csd.pm.exceptions.PMException;
import gov.nist.csd.pm.graph.Graph;
import gov.nist.csd.pm.graph.GraphSerializer;
import gov.nist.csd.pm.graph.MemGraph;
import gov.nist.csd.pm.graph.model.nodes.Node;
import gov.nist.csd.pm.graph.model.nodes.NodeType;
import gpms.model.GPMSCommonInfo;
import gpms.rest.UserService;

import java.io.File;
import java.io.IOException;
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

import static gov.nist.csd.pm.graph.model.nodes.NodeType.O;
import static gov.nist.csd.pm.graph.model.nodes.NodeType.OA;
import static gov.nist.csd.pm.graph.model.nodes.NodeType.U;
import static gov.nist.csd.pm.graph.model.nodes.NodeType.UA;

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
	
	private static final Logger log = Logger.getLogger(PDSOperations.class.getName());
	
	public PDSOperations()
	{
		this.ngacPolicy = NGACPolicyConfigurationLoader.getPolicy();
		gpmsNgacObligations = new GpmsNgacObligations();
	}
	
	
	/**
	 * This function checks whether a user has permission for a task
	 * @param userInfo
	 * @return true/false
	 */
	public boolean hasPermissionToCreateAProposal(GPMSCommonInfo userInfo)
	{
		boolean hasPermission = true;		
		
		HashMap map = Task.CREATE_PROPOSAL.getPermissionsSets();
		
		Iterator<Map.Entry<Attribute, HashSet>> itr = map.entrySet().iterator(); 
         
        while(itr.hasNext()) 
        { 
             Map.Entry<Attribute, HashSet> entry = itr.next(); 
             log.info("Container = " + entry.getKey() +  
                                 ", permission set = " + entry.getValue()); 
             hasPermission = UserPermissionChecker.checkPermission(ngacPolicy, userInfo.getUserName(), (Attribute)entry.getKey(), entry.getValue().toArray());
        } 
        
		log.info("Create Proposal Permission : "+hasPermission);
		
		return hasPermission;
	}
	
	
	
	public long createAProposal(String userName) throws PMException
	{
		printAccessState("Before User creates PDS", ngacPolicy);
		long randomId = getID();
		//user creates a PDS and assigns it to Constants.PDS_ORIGINATING_OA
        Node pdsNode = this.ngacPolicy.createNode(randomId, createProposalId(randomId), OA, null);
        
        long pdsOriginationOAID = getNodeID(ngacPolicy, Constants.PDS_ORIGINATING_OA, OA, null);
        ngacPolicy.assign(pdsNode.getID(), pdsOriginationOAID);
       
        
        long userID = getNodeID(ngacPolicy, userName, U, null);
        simulateAssignToEvent(ngacPolicy, userID, ngacPolicy.getNode(pdsOriginationOAID), pdsNode);

        printAccessState("After User creates PDS", ngacPolicy);
        
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
    private static void printAccessState(String step, Graph graph) throws PMException {
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
            Map<Long, Set<String>> accessibleNodes = decider.getAccessibleNodes(user.getID());
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
     * @param graph the graph
     * @param userID the ID of the user that triggered the event
     * @param targetNode the node that the event happens on
     * @throws PMException
     */
    private void simulateAssignToEvent(Graph graph, long userID, Node targetNode, Node childNode) throws PMException {
        // check if the target of the event is a particular container and execute the corresponding "response"
        if(targetNode.getID() == this.getNodeID(graph, Constants.PDS_ORIGINATING_OA, OA, null)) {
            gpmsNgacObligations.createPDS(graph, userID, childNode);
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
	
	private boolean isChildrenFound(String name,String parent) throws PMException
    {
    	boolean found = false;
        // get all of the users in the graph
        Set<Node> search = ngacPolicy.search(parent, UA.toString(), null);
        
        System.out.println(search.size());
        
        for(Node userAttNode : search) {
        	
        	 Set<Long> childIds = ngacPolicy.getChildren(userAttNode.getID());
        	 log.info("No of Children Assigned on "+parent+" :"+childIds.size()+"|"+childIds);
        	 
        	 long sourceNode = this.getNodeID(ngacPolicy, name, U, null);
        	 
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
