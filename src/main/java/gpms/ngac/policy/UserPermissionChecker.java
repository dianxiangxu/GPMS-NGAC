package gpms.ngac.policy;


import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.O;
import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.OA;
import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.U;
import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.UA;

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
import gov.nist.csd.pm.pip.prohibitions.model.Prohibition;

public class UserPermissionChecker {
	
	private static final Logger log = Logger.getLogger(UserPermissionChecker.class.getName());
	
	public static boolean checkPermissionAnyType(Graph graph, Prohibitions prohibitions, String userOrAttributeName,String type,Attribute targetAttribute, List<String> objects) {
		
		boolean hasPermission = false;
		try {
			String pro = ProhibitionsSerializer.toJson(prohibitions);
			log.info("Prohibition:"+pro.length());
			//log.info("Graph Policy:"+ GraphSerializer.toJson(ngacPolicy));

			PReviewDecider decider = new PReviewDecider(graph,prohibitions);
			
			//long targetId = PDSOperations.getNodeID(ngacPolicy, targetAttribute.getAttributeName(), targetAttribute.getAttributeType(), null);
			
			// get all of the users in the graph
	        Node user = graph.getNode(userOrAttributeName);
	        
	        //Set<Node> uaSet = ngacPolicy.search(userName, UA.toString(), null);
	        
	        
	        	String[] requiredAccessRights = objects.toArray(new String[0]) ;//Arrays.copyOf(objects, objects.size(), String[].class);
	   		    //objects.to
	        	
	        		 log.info("UserPermissionChecker: "+user.getName()+"|"+requiredAccessRights.toString());
	        		 //System.out.println("UserPermissionChecker: "+user.getName()+"|"+targetAttribute.toString()+"|"+Arrays.toString(requiredAccessRights));
	        		 hasPermission = decider.check(userOrAttributeName, "process" ,targetAttribute.getAttributeName(), requiredAccessRights);
	        		 
	        		 log.info(hasPermission);
	        		 //System.out.println("Permission:"+hasPermission);
	        	 

        } catch(PMException pme) {
        	log.debug("PM Exception:"+pme.getMessage());
        	pme.printStackTrace();
        }        
       
		return hasPermission;
	}
	
public static boolean checkPermission(Graph graph, Prohibitions prohibitions, String userName,Attribute targetAttribute, String[] objects) {
		
		boolean hasPermission = false;
		try {
			
			String pro = ProhibitionsSerializer.toJson(prohibitions);
			log.info("Prohibition:"+pro);
			//log.info("Graph Policy:"+ GraphSerializer.toJson(ngacPolicy));

			PReviewDecider decider = new PReviewDecider(graph,prohibitions);
			
			//long targetId = PDSOperations.getNodeID(ngacPolicy, targetAttribute.getAttributeName(), targetAttribute.getAttributeType(), null);
			
			// get all of the users in the graph
	        Node user = graph.getNode(userName);	        	        
	        	//String[] requiredAccessRights = Arrays.copyOf(objects, objects.length, String[].class);	
	        
	        for(String s : objects) {
		    log.info("UserPermissionChecker: "+user.getName()+"|"+s+"|" +targetAttribute.getAttributeName());
		    
	        }

    		//System.out.println("UserPermissionChecker: "+user.getName()+"|"+targetAttribute.toString()+"|"+Arrays.toString(objects));
    	    hasPermission = decider.check(userName, "process" , targetAttribute.getAttributeName(), objects);
    		log.info(hasPermission);	        
        } catch(PMException pme) {
        	log.debug("PM Exception:"+pme.getMessage());
        	pme.printStackTrace();
        }        
       
		return hasPermission;
	}

}
