package gpms.ngac.policy;

import static gov.nist.csd.pm.graph.model.nodes.NodeType.U;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import gov.nist.csd.pm.decider.PReviewDecider;
import gov.nist.csd.pm.exceptions.PMException;
import gov.nist.csd.pm.graph.Graph;
import gov.nist.csd.pm.graph.model.nodes.Node;

public class UserTaskPermissionOperations {

	private static final Logger log = Logger.getLogger(UserTaskPermissionOperations.class
			.getName());
	
	private static Graph ngacPolicy;
	
	
	public static void init() {
		if(ngacPolicy == null )
			ngacPolicy = NGACPolicyConfigurationLoader.getPolicy();
	}


	public static void populateUsersApprovedTaskSet(String userName) {
		
		log.info("PopulateUsersApprovedTaskSet()"+userName);
		try {
			HashMap hashMap = getAllAccessState(userName,ngacPolicy);
			
			//we are iterating all the defined tasks from the TaskConfigurationParser class
			for(TaskDefinition taskDefinition : TaskConfigurationParser.getTasks()) {
				
				log.info("Task permission check:"+taskDefinition.getName());
				
				Iterator<Map.Entry<String, HashSet>> itr = taskDefinition.accumulateAllAccessRightsSet().entrySet().iterator();		          
		        
				boolean goodforCheck = true;
				while(itr.hasNext() && goodforCheck) 
		        { 
		             Map.Entry<String, HashSet> entry = itr.next(); 
		             String key = entry.getKey();
		             HashSet valueSet = entry.getValue();
		             log.info("Key = " + key +  ", Value set = " + valueSet); 
		             
		             HashSet refenenceSet = (HashSet)hashMap.get(key);
		             log.info("Key = " + key +  ", Reference Value set = " + refenenceSet); 
		             
		             if(!valueSet.containsAll(refenenceSet)) {
		            	 goodforCheck = false;
		             }
		            
		        } 
				
				if(goodforCheck) {
					UserTaskPermissionRepo.add(userName, taskDefinition.getName());
				} else {
					goodforCheck = true;
				}
				
			}
			
			
			
			
		} catch (PMException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
     * 
     * @param userName the name of the user
     * @param graph the graph to determine permissions
     */
    private static HashMap<String, HashSet> getAllAccessState(String userName, Graph graph) throws PMException {
        log.info("############### Access rights for " + userName + " ###############");

        HashMap<String, HashSet> allAccessRights = new HashMap<String,HashSet>();
        
        // initialize a PReviewDecider to make decisions
        PReviewDecider decider = new PReviewDecider(graph);

        // get all of the users in the graph
        Set<Node> search = graph.search(userName, U.toString(), null);
        for(Node user : search) {   //get the user with the userName; potentially will get one
           
        	log.info("UserTaskPermissionOperations: "+user.getName());
            // get all of the nodes accessible for the current user
            Map<Long, Set<String>> accessibleNodes = decider.getAccessibleNodes(user.getID());
            for(long objectID : accessibleNodes.keySet()) {
                Node obj = graph.getNode(objectID);
                log.info("\t" + obj.getName() + obj.getType().name()+" -> " + accessibleNodes.get(objectID));
                
                String key = obj.getName()+obj.getType().name();  // key = nodeName+type
    			if(allAccessRights.containsKey(key)) {
    				
    				HashSet set = (HashSet)allAccessRights.get(key);				
    				set.addAll(accessibleNodes.get(objectID));
    				allAccessRights.put(key,set);
    			}
    			else {
    				
    				HashSet set = (HashSet) accessibleNodes.get(objectID);
    				allAccessRights.put(key,set);
    			}
            }
        }
        log.info("############### End Access state for " + userName + "############");
        log.info(allAccessRights);
        return allAccessRights;
    }
}
