package gpms.pds;

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
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;

import static gov.nist.csd.pm.graph.model.nodes.NodeType.O;
import static gov.nist.csd.pm.graph.model.nodes.NodeType.OA;
import static gov.nist.csd.pm.graph.model.nodes.NodeType.U;
import static gov.nist.csd.pm.graph.model.nodes.NodeType.UA;


public class PDSOperations {
	
	private Graph graph;
	
	private static final Logger log = Logger.getLogger(PDSOperations.class
			.getName());
	
	public PDSOperations()
	{
		this.graph = InitialConfigurationLoader.getGraph();
	}
	
	public boolean shouldCheckIfUserBelongsToTenure(HashMap<String,String> attr)
	{
		if(attr.get("position.type").equals("Tenured/tenure-track faculty") && 
				attr.get("proposal.section").equalsIgnoreCase("Whole Proposal") &&
				attr.get("proposal.action").equalsIgnoreCase("Add"))
			return true;
		return false;
	}
	
	public boolean hasPermission(GPMSCommonInfo userInfo)
	{
		boolean ret = false;
		try {
			ret = isChildrenFound(userInfo.getUserName(),"tenure") ;
		} catch (PMException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return ret;
	}
	
	private boolean isChildrenFound(String name,String parent) throws PMException
    {
    	boolean found = false;
        // get all of the users in the graph
        Set<Node> search = graph.search(parent, UA.toString(), null);
        
        System.out.println(search.size());
        
        for(Node userAttNode : search) {
        	
        	 Set<Long> childIds = graph.getChildren(userAttNode.getID());
        	 log.info("No of Children Assigned on "+parent+" :"+childIds.size()+"|"+childIds);
        	 
        	 long tenureFacultyNode = this.getNodeID(graph, name, U, null);
        	 
        	 log.info("We are looking for:"+tenureFacultyNode);
        	 
        	 if(childIds.contains(tenureFacultyNode))
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
	
	public long getNodeID(Graph graph, String name, NodeType type, Map<String, String> properties) throws PMException {
        Set<Node> search = graph.search(name, type.toString(), properties);
        if(search.isEmpty()) {
            throw new PMException("no node with name " + name + ", type " + type + ", and properties " + properties);
        }

        return search.iterator().next().getID();
    }


}
