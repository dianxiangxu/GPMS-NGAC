package gpms.ngac.policy;


import static gov.nist.csd.pm.graph.model.nodes.NodeType.U;
import static gov.nist.csd.pm.graph.model.nodes.NodeType.UA;
import static gov.nist.csd.pm.graph.model.nodes.NodeType.OA;
import static gpms.dev.PDS.getNodeID;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import gov.nist.csd.pm.decider.PReviewDecider;
import gov.nist.csd.pm.exceptions.PMException;
import gov.nist.csd.pm.graph.Graph;
import gov.nist.csd.pm.graph.model.nodes.Node;

public class UserPermissionChecker {
	
	private static final Logger log = Logger.getLogger(UserPermissionChecker.class.getName());
	
	public static boolean checkPermission(Graph ngacPolicy, String userName,Attribute targetAttribute, Object[] objects) {
		
		boolean hasPermission = false;
		try {
			PReviewDecider decider = new PReviewDecider(ngacPolicy);
			
			long targetId = getNodeID(ngacPolicy, targetAttribute.getAttributeName(), targetAttribute.getAttributeType(), null);
			
			// get all of the users in the graph
	        Set<Node> userSet = ngacPolicy.search(userName, U.toString(), null);
	        
	        if(userSet.size() ==1)   // expect to get only one user
	        {
	        	String[] requiredAccessRights = Arrays.copyOf(objects, objects.length, String[].class);
	   		 
	        	 for(Node user : userSet) {
	        		 log.info("UserTaskPermissionOperations: "+user.getName()+"|"+requiredAccessRights);
	        		 hasPermission = decider.hasPermissions(user.getID(), targetId, requiredAccessRights);
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
