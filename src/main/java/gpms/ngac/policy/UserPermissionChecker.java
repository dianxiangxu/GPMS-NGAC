package gpms.ngac.policy;


import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.O;
import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.OA;
import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.U;
import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.UA;
import static gpms.ngac.policy.PDSOperations.getNodeID;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import gov.nist.csd.pm.pdp.decider.PReviewDecider;
import gov.nist.csd.pm.pip.graph.Graph;
import gov.nist.csd.pm.pip.graph.MemGraph;
import gov.nist.csd.pm.exceptions.PMException;
import gov.nist.csd.pm.pip.graph.model.nodes.Node;
import gov.nist.csd.pm.pip.graph.model.nodes.NodeType;

public class UserPermissionChecker {
	
	private static final Logger log = Logger.getLogger(UserPermissionChecker.class.getName());
	
	public static boolean checkPermission(Graph ngacPolicy, String userName,Attribute targetAttribute, Object[] objects) {
		
		boolean hasPermission = false;
		try {
			PReviewDecider decider = new PReviewDecider(ngacPolicy);
			
			long targetId = PDSOperations.getNodeID(ngacPolicy, targetAttribute.getAttributeName(), targetAttribute.getAttributeType(), null);
			
			// get all of the users in the graph
	        Set<Node> userSet = ngacPolicy.search(userName, U.toString(), null);
	        
	        if(userSet.size() ==1)   // expect to get only one user
	        {
	        	String[] requiredAccessRights = Arrays.copyOf(objects, objects.length, String[].class);
	   		 
	        	 for(Node user : userSet) {
	        		 log.info("UserTaskPermissionOperations: "+user.getName()+"|"+requiredAccessRights);
	        		 hasPermission = decider.check(user.getID(), 123 ,targetId, requiredAccessRights);
	        		 
	        		 log.info(hasPermission);
	        	 }
	        }
	        else
	        {
	        	log.info("User set size:"+userSet.size());
	        }
        } catch(PMException pme) {
        	log.debug("PM Exception:"+pme.getMessage());
        	pme.printStackTrace();
        }        
       
		return hasPermission;
	}
	
public static boolean checkPermission2(Graph ngacPolicy, String userName,Attribute targetAttribute, String[] objects) {
		
		boolean hasPermission = false;
		try {
			PReviewDecider decider = new PReviewDecider(ngacPolicy);
			
			long targetId = PDSOperations.getNodeID(ngacPolicy, targetAttribute.getAttributeName(), targetAttribute.getAttributeType(), null);
			
			// get all of the users in the graph
	        Set<Node> userSet = ngacPolicy.search(userName, U.toString(), null);
	        
	        if(userSet.size() ==1)   // expect to get only one user
	        {
	        	//String[] requiredAccessRights = Arrays.copyOf(objects, objects.length, String[].class);
	   		 
	        	 for(Node user : userSet) {
	        		 log.info("UserTaskPermissionOperations: "+user.getName()+"|"+objects);
	        		 hasPermission = decider.check(user.getID(), 124 ,targetId, objects);
	        		 log.info(hasPermission);
	        		
	        	 }
	        }
	        else
	        {
	        	log.info("User set size:"+userSet.size());
	        }
        } catch(PMException pme) {
        	log.debug("PM Exception:"+pme.getMessage());
        	pme.printStackTrace();
        }        
       
		return hasPermission;
	}

}
