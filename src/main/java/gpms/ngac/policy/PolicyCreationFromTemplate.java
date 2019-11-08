package gpms.ngac.policy;


import static gpms.dev.PDS.getID;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import gov.nist.csd.pm.exceptions.PMException;
import gov.nist.csd.pm.pip.graph.Graph;
import gov.nist.csd.pm.pip.graph.model.nodes.Node;
import gov.nist.csd.pm.pip.graph.model.nodes.NodeType;

import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.O;
import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.OA;
import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.U;
import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.UA;

public class PolicyCreationFromTemplate {

	private static final Logger log = Logger.getLogger(PolicyCreationFromTemplate.class.getName());

	public PolicyCreationFromTemplate() {
	}

	public Node createPolicyFromTemplate(Graph ngacPolicy, Node iPdsNode) {

		long pdsId = iPdsNode.getID();
		log.info("PDS ID:" + pdsId);

		HashMap<String, Node> nodeMap = new HashMap<String, Node>();

		try {
			File file = getFileFromResources(Constants.PDS_TEMPLATE);
			Object obj = new JSONParser().parse(new FileReader(file));
			JSONObject jasonObject = (JSONObject) obj;

			JSONArray jasonArrayNodes = (JSONArray) jasonObject.get("nodes");
			createNodes(ngacPolicy, pdsId, jasonArrayNodes, nodeMap);

			JSONArray jasonArrayAssignments = (JSONArray) jasonObject.get("assignments");
			createAssignments(ngacPolicy, pdsId, jasonArrayAssignments, iPdsNode, nodeMap);

			JSONArray jasonArrayAssociations = (JSONArray) jasonObject.get("associations");
			createAssociations(ngacPolicy, pdsId, jasonArrayAssociations, iPdsNode, nodeMap);

			//printNodes(nodeMap);			 

		} catch (FileNotFoundException fnf) {
			log.info("task definition file not found");
		} catch (IOException io) {
			log.info("IO error");
		} catch (ParseException pe) {
			log.info("Parser Exception");
		} catch (PMException pme) {
			log.info("PM Exception");
		}

		return iPdsNode;
	}

	private void createNodes(Graph ngacPolicy, long pdsId, JSONArray jasonArrayNodes, HashMap<String, Node> nodeMap) throws PMException {
		Iterator iteratorNode = jasonArrayNodes.iterator();

		while (iteratorNode.hasNext()) {
			JSONObject nodeObj = (JSONObject) iteratorNode.next();

			String id = (String) nodeObj.get("id");
			String name = (String) nodeObj.get("name");
			String type = (String) nodeObj.get("type");

			NodeType nodeType = null;
			if (type.equalsIgnoreCase("OA"))
				nodeType = OA;
			else if (type.equalsIgnoreCase("UA"))
				nodeType = UA;

			Node node = ngacPolicy.createNode(getID(), name + pdsId, nodeType, null);
			nodeMap.put(id, node);
			System.out.println(node.getName());

		}
	}
	
	private void createAssignments(Graph ngacPolicy, long pdsId, JSONArray jasonArrayAssignments,Node iPdsNode, HashMap<String, Node> nodeMap) throws PMException {
		Iterator iteratorAssignment = jasonArrayAssignments.iterator();

		while (iteratorAssignment.hasNext()) {
			JSONObject nodeObj = (JSONObject) iteratorAssignment.next();

			String source = (String) nodeObj.get("source");
			String target = (String) nodeObj.get("target");
			
			if(target.equalsIgnoreCase("0"))  
			{
				ngacPolicy.assign(nodeMap.get(source).getID(), iPdsNode.getID());
				/*if(nodeMap.get(source).getType() == UA && nodeMap.get(target).getType() == OA)
				{
					ngacPolicy.assign(nodeMap.get(source).getID(), -8343754015336576481l );
				}
				else
				{
					
					
				}*/
			}
			else if (target.equalsIgnoreCase("-1")) {
				ngacPolicy.assign(nodeMap.get(source).getID(), -8343754015336576481l );
			}
			else
				ngacPolicy.assign(nodeMap.get(source).getID(), nodeMap.get(target).getID());
				

		}
	}
	
	private void createAssociations(Graph ngacPolicy, long pdsId, JSONArray jasonArrayAssociations, Node iPdsNode, HashMap<String, Node> nodeMap) throws PMException {
		Iterator iteratorAssociation = jasonArrayAssociations.iterator();

		while (iteratorAssociation.hasNext()) {
			JSONObject associationObj = (JSONObject) iteratorAssociation.next();

			String source = (String) associationObj.get("source");
			String target = (String) associationObj.get("target");
			
			JSONArray accessRightsArr = (JSONArray) associationObj.get("access_right_set"); 
			//log.info();
            
            Iterator itrAccessRights = accessRightsArr.iterator(); 
            ArrayList<String> listdata = new ArrayList<String>();   
            
            while (itrAccessRights.hasNext()) { 
           	 String accessRight = (String) itrAccessRights.next();
           	 listdata.add(accessRight);		                	 
            } 
            
            log.info("List Data:"+listdata);
            
            if(target.equalsIgnoreCase("0"))  
            	ngacPolicy.associate(nodeMap.get(source).getID(), iPdsNode.getID(), new HashSet<>(listdata));            	
			else
				ngacPolicy.associate(nodeMap.get(source).getID(), nodeMap.get(target).getID(), new HashSet<>(listdata));
				
            
    		

		}
	}
	
	private void printNodes( HashMap<String, Node> nodeMap) {
		Iterator<Map.Entry<String, Node>> iterator = nodeMap.entrySet().iterator();

		while (iterator.hasNext()) {
			Entry<String, Node> entry = iterator.next();
			System.out.println("Key = " + entry.getKey() + ", Value = " + entry.getValue());
		}
	}

	
	private File getFileFromResources(String fileName) {
		ClassLoader classLoader = this.getClass().getClassLoader();

		URL resource = classLoader.getResource(fileName);
		if (resource == null) {
			throw new IllegalArgumentException("file is not found!");
		} else {
			return new File(resource.getFile());
		}

	}
}
