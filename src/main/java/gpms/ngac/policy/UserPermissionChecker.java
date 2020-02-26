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
import gov.nist.csd.pm.pip.graph.GraphSerializer;
import gov.nist.csd.pm.pip.graph.MemGraph;
import gov.nist.csd.pm.exceptions.PMException;
import gov.nist.csd.pm.pip.graph.model.nodes.Node;
import gov.nist.csd.pm.pip.graph.model.nodes.NodeType;
import gov.nist.csd.pm.pip.prohibitions.Prohibitions;
import gov.nist.csd.pm.pip.prohibitions.ProhibitionsSerializer;

public class UserPermissionChecker {
	
	private static final Logger log = Logger.getLogger(UserPermissionChecker.class.getName());
	
	public static boolean checkPermissionAnyType(Graph ngacPolicy, Prohibitions prohibitions, String userOrAttributeName,String type,Attribute targetAttribute, List<String> objects) {
		
		boolean hasPermission = false;
		try {
			String pro = ProhibitionsSerializer.toJson(prohibitions);
			log.info("Prohibition:"+pro.length());
			//log.info("Graph Policy:"+ GraphSerializer.toJson(ngacPolicy));

			PReviewDecider decider = new PReviewDecider(ngacPolicy,prohibitions);
			
			long targetId = PDSOperations.getNodeID(ngacPolicy, targetAttribute.getAttributeName(), targetAttribute.getAttributeType(), null);
			
			// get all of the users in the graph
	        Set<Node> userSet = ngacPolicy.search(userOrAttributeName, type, null);
	        
	        //Set<Node> uaSet = ngacPolicy.search(userName, UA.toString(), null);
	        
	        if(userSet.size() ==1)   // expect to get only one user
	        {
	        	String[] requiredAccessRights = objects.toArray(new String[0]) ;//Arrays.copyOf(objects, objects.size(), String[].class);
	   		    //objects.to
	        	
	        	 for(Node user : userSet) {
	        		 log.info("UserPermissionChecker: "+user.getName()+"|"+requiredAccessRights.toString());
	        		 System.out.println("UserPermissionChecker: "+user.getName()+"|"+targetAttribute.toString()+"|"+Arrays.toString(requiredAccessRights));
	        		 hasPermission = decider.check(user.getID(), NGACPolicyConfigurationLoader.getID() ,targetId, requiredAccessRights);
	        		 
	        		 log.info(hasPermission);
	        		 System.out.println("Permission:"+hasPermission);
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
	
public static boolean checkPermission(Graph ngacPolicy, Prohibitions prohibitions, String userName,Attribute targetAttribute, String[] objects) {
		
		boolean hasPermission = false;
		try {
			
			String pro = ProhibitionsSerializer.toJson(prohibitions);
			log.info("Prohibition:"+pro.length());
			//log.info("Graph Policy:"+ GraphSerializer.toJson(ngacPolicy));

			PReviewDecider decider = new PReviewDecider(ngacPolicy,prohibitions);
			
			long targetId = PDSOperations.getNodeID(ngacPolicy, targetAttribute.getAttributeName(), targetAttribute.getAttributeType(), null);
			
			// get all of the users in the graph
	        Set<Node> userSet = ngacPolicy.search(userName, U.toString(), null);
	        
	        if(userSet.size() ==1)   // expect to get only one user
	        {
	        	//String[] requiredAccessRights = Arrays.copyOf(objects, objects.length, String[].class);
	   		 
	        	 for(Node user : userSet) {
	        		 log.info("UserPermissionChecker: "+user.getName()+"|"+objects.toString());
	        		 System.out.println("UserPermissionChecker: "+user.getName()+"|"+targetAttribute.toString()+"|"+Arrays.toString(objects));
	        		 hasPermission = decider.check(user.getID(), NGACPolicyConfigurationLoader.getID() ,targetId, objects);
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
