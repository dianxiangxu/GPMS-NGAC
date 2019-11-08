package gpms.dev;

import gov.nist.csd.pm.epp.events.AssignToEvent;
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
import gov.nist.csd.pm.pip.obligations.evr.EVRException;
import gov.nist.csd.pm.pip.obligations.evr.EVRParser;
import gov.nist.csd.pm.pip.obligations.model.Condition;
import gov.nist.csd.pm.pip.obligations.model.EventPattern;
import gov.nist.csd.pm.pip.obligations.model.EvrNode;
import gov.nist.csd.pm.pip.obligations.model.Obligation;
import gov.nist.csd.pm.pip.obligations.model.ResponsePattern;
import gov.nist.csd.pm.pip.obligations.model.Rule;
import gov.nist.csd.pm.pip.obligations.model.Subject;
import gov.nist.csd.pm.pip.obligations.model.Target;
import gov.nist.csd.pm.pip.obligations.model.actions.Action;
import gov.nist.csd.pm.pip.obligations.model.actions.CreateAction;
import gov.nist.csd.pm.pip.obligations.model.functions.Arg;
import gov.nist.csd.pm.pip.prohibitions.MemProhibitions;

import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.O;
import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.OA;
import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.U;
import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.UA;
import gpms.ngac.policy.Constants;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Random;
import java.util.Set;



public class PDS {

    public static Random rand = new Random();
    

    public static void main(String[] args) throws IOException, PMException {
        // load the initial configuration from json
    	
     
    	PDS main = new PDS();
    	//File file = main.getFileFromResources(main,"docs/super_config.json");
    	
		
		  File file = main.getFileFromResources(main,"docs/super_config.json"); 
		  File file2 = main.getFileFromResources(main,"docs/proposal_creation.json"); 
		  File file3 = main.getFileFromResources(main,"docs/university_organization.json"); 
		  File file4 = main.getFileFromResources(main,"docs/editing_policy_before_submission.json"); 
		  File file5 = main.getFileFromResources(main,"docs/create_proposal.yml"); 
		 
		String json = new String(Files.readAllBytes(Paths.get(file.getAbsolutePath())));
        String json2 = new String(Files.readAllBytes(Paths.get(file2.getAbsolutePath())));
        String json3 = new String(Files.readAllBytes(Paths.get(file3.getAbsolutePath())));
        String json4 = new String(Files.readAllBytes(Paths.get(file4.getAbsolutePath())));
        //String yml = new String(Files.readAllBytes(Paths.get(file4.getAbsolutePath())));
     
        Graph graph =null;
        try {
        graph = GraphSerializer.fromJson(new MemGraph(), json);
        graph = GraphSerializer.fromJson(graph, json2);
        graph = GraphSerializer.fromJson(graph, json3);
        graph = GraphSerializer.fromJson(graph, json4);
        System.out.println("Nodes:"+graph.getNodes().size());
        printAccessState("Initial configuration", graph);
        } catch(Exception e)
        {
        	System.out.println(e.toString());
        	e.printStackTrace();
        }
        

		long randomId = getID(); 
		Node pdsNode = graph.createNode(randomId, ""+randomId, OA, null);
		
		long pdsOriginationOAID = getNodeID(graph, Constants.PDS_ORIGINATING_OA,  OA, null); 
		
		Obligation obligation=null;
		
		try{
    		InputStream is = new FileInputStream(file5);
    		obligation = EVRParser.parse(is);
    		
    		PDP pdp = new PDP(new PAP(graph, new MemProhibitions(), new MemObligations()));
    	 	  
    	       
	        pdp.getPAP().getObligationsPAP().add(obligation, true);

	       // test u1 assign to
	        long userID = getNodeID(graph, "nazmul", U, null);
	        pdp.getEPP().processEvent(new AssignToEvent(graph.getNode(pdsOriginationOAID), pdsNode),userID, 123);
		
		} catch(Exception e) {
			System.out.println(e.toString());
		}
		
		 printAccessState("Initial configuration", graph);
		//ngacPolicy.assign(pdsNode.getID(), pdsOriginationOAID);
		//graph.assign(pdsNode.getID(), pdsOriginationOAID);
		
		//long userID = getNodeID(graph, userName, U, null);
		//simulateAssignToEvent(graph, userID, graph.getNode(pdsOriginationOAID), pdsNode);
		
		
        
        
		
//		 System.out.println(getID()); System.out.println(getID());
//		 System.out.println(getID()); System.out.println(getID());
//		 System.out.println(getID()); System.out.println(getID());
//		 System.out.println(getID()); System.out.println(getID());
//		 System.out.println(getID()); System.out.println(getID());
//		 System.out.println(getID()); System.out.println(getID());
        
        /*String json = new String(Files.readAllBytes(Paths.get(file.getAbsolutePath())));
        String json2 = new String(Files.readAllBytes(Paths.get(file2.getAbsolutePath())));
        String json3 = new String(Files.readAllBytes(Paths.get(file3.getAbsolutePath())));
        String json4 = new String(Files.readAllBytes(Paths.get(file4.getAbsolutePath())));
        String json5 = new String(Files.readAllBytes(Paths.get(file5.getAbsolutePath())));
        */
    	//String json6 = new String(Files.readAllBytes(Paths.get(file6.getAbsolutePath())));

        
        //System.out.println(json);
    //    Graph graph = GraphSerializer.fromJson(new MemGraph(), json1);
    //   graph = GraphSerializer.fromJson(graph, json2);
        
       /* Graph graph = GraphSerializer.fromJson(new MemGraph(), json);
        graph = GraphSerializer.fromJson(graph, json2);
        graph = GraphSerializer.fromJson(graph, json3);
        graph = GraphSerializer.fromJson(graph, json4);
        graph = GraphSerializer.fromJson(graph, json5);
        */
        
        //File file4 =
		/* * main.getFileFromResources(main,"docs/approval_config.json"); File file5 =
		 * main.getFileFromResources(main,"docs/cross_policy.json");
		 */
    	//File file6 = main.getFileFromResources(main,"docs/pds_template_updated2.json");

      //  printFile(file);
    	
       // String json = new String(Files.readAllBytes(Paths.get("/resources/docs/pds.json")));
       
        
       // System.out.println(json6);       
		/*
		 * long randomId = getID(); //user creates a PDS and assigns it to
		 * Constants.PDS_ORIGINATING_OA Node pdsNode = graph.createNode(randomId,
		 * createProposalId(randomId), OA, null);
		 * 
		 * long pdsOriginationOAID = getNodeID(graph, Constants.PDS_ORIGINATING_OA, OA,
		 * null); graph.assign(pdsNode.getID(), pdsOriginationOAID);
		 * 
		 * String userName = "nazmul";
		 * 
		 * 
		 * 
		 * long userID = getNodeID(graph, userName, U, null);
		 * simulateAssignToEvent(graph, userID, graph.getNode(pdsOriginationOAID),
		 * pdsNode);
		 * 
		 * printAccessState("After User creates PDS", graph);
		 * 
		 */
        
		/*
		 * // ----------------writing policy to json-------------------- String
		 * policyString = GraphSerializer.toJson(graph);
		 * 
		 * // System.out.println(policyString); // Files.write("", policyString.to,
		 * options)
		 * 
		 * // File file6 = main.getFileFromResources(main,"docs/test.json"); File file6
		 * = new File("docs/test.json"); BufferedWriter writer = null; try { writer =
		 * new BufferedWriter( new FileWriter(file6)); writer.write( policyString);
		 * writer.flush();
		 * 
		 * } catch ( IOException e) { e.printStackTrace(); } catch(Exception ex) {
		 * ex.printStackTrace(); } finally { try { if ( writer != null) writer.close( );
		 * } catch ( IOException e) { e.printStackTrace(); } }
		 * //-----------------writing ends here------------------
		 */      
        
       
       
       
       
       
       
       
       

       // boolean found = isChildrenFound("bob","tenure",graph);
        
      // printAccessStateForUA("Initial configuration", graph);
        
    
        
      //  printAccessState("Initial configuration", graph);

		/*
		 * // Step 1. Bob creates a PDS and assigns it to RBAC_PDSs Node pdsNode =
		 * graph.createNode(getID(), "PDSi", OA, null); long rbacPDSsID =
		 * getNodeID(graph, "RBAC_PDSs", OA, null); graph.assign(pdsNode.getID(),
		 * rbacPDSsID); // simulate an event // normally the Event Processing Point will
		 * do this, so we'll just simulate it by // calling the simulateAssignToEvent
		 * method long bobID = getNodeID(graph, "bob", U, null);
		 * simulateAssignToEvent(graph, bobID, graph.getNode(rbacPDSsID), pdsNode);
		 * 
		 * p
		 rintAccessState("After bob creates PDS", graph);
		 */
    }
    
    
    private static String createProposalId(long id)
	{
		return "PDS"+id;
	}
    
    /*
        // Step 2. Bob adds alice as a CoPI
        Node aliceObj = graph.createNode(getID(), "alice", O, null);
        long copiID = getNodeID(graph, "CoPI", OA, null);
        graph.assign(aliceObj.getID(), copiID);
        // simulate an event
        simulateAssignToEvent(graph, bobID, graph.getNode(copiID), aliceObj);

        printAccessState("After bob adds alice as a CoPI", graph);

        // Step 3. Alice adds Charlie as a CoPI
        Node charlieObj = graph.createNode(getID(), "charlie", O, null);
        long spID = getNodeID(graph, "SP", OA, null);
        graph.assign(charlieObj.getID(), spID);
        // simulate an event
        long aliceID = getNodeID(graph, "alice", U, null);
        simulateAssignToEvent(graph, aliceID, graph.getNode(spID), charlieObj);

        printAccessState("After alice adds charlie as a SP", graph);

        // Step 4. Bob submits the PDS for approval
        long submittedPDSs = getNodeID(graph, "submitted_pdss", OA, null);
        graph.assign(pdsNode.getID(), submittedPDSs);
        // simulate an event
        simulateAssignToEvent(graph, bobID, graph.getNode(submittedPDSs), pdsNode);

        printAccessState("After bob submits the PDS for approval", graph);

        // Step 5. CS Chair approves
        long csChairApproval = getNodeID(graph, "cs_chair_approval", OA, null);
        graph.assign(pdsNode.getID(), csChairApproval);
        // simulate an event
        simulateAssignToEvent(graph, getNodeID(graph, "CS_Chair", U, null), graph.getNode(csChairApproval), pdsNode);

        printAccessState("After the CS Chair approves the PDS", graph);

        // Step 6. Math Chair approves
        long mathChairApproval = getNodeID(graph, "math_chair_approval", OA, null);
        graph.assign(pdsNode.getID(), mathChairApproval);
        // simulate an event
        simulateAssignToEvent(graph, getNodeID(graph, "Math_Chair", U, null), graph.getNode(mathChairApproval), pdsNode);

        printAccessState("After Math Chair approves the PDS", graph);

        // Step 7. COEN Dean approves
        long coenDeanApproval = getNodeID(graph, "coen_dean_approval", OA, null);
        graph.assign(pdsNode.getID(), coenDeanApproval);
        // simulate an event
        simulateAssignToEvent(graph, getNodeID(graph, "COEN_Dean", U, null), graph.getNode(coenDeanApproval), pdsNode);

        printAccessState("After the COEN Dean approves the PDS", graph);

        // Step 8. COAS Dean approves
        long coasDeanApproval = getNodeID(graph, "coas_dean_approval", OA, null);
        graph.assign(pdsNode.getID(), coasDeanApproval);
        // simulate an event
        simulateAssignToEvent(graph, getNodeID(graph, "COAS_Dean", U, null), graph.getNode(coasDeanApproval), pdsNode);

        printAccessState("After the COAS Dean approves the PDS", graph);
    }*/
    
    private static boolean isChildrenFound(String name,String parent, Graph graph) throws PMException
    {
    	boolean found = false;
        // get all of the users in the graph
        Set<Node> search = graph.search(parent, UA.toString(), null);
        
        System.out.println(search.size());
        
        for(Node userAttNode : search) {
        	
        	 Set<Long> childIds = graph.getChildren(userAttNode.getID());
        	 System.out.println(childIds.size()+"|"+childIds);
        	 
        	 long tenureFacultyNode = getNodeID(graph, name, U, null);
        	 
        	 System.out.println(tenureFacultyNode);
        	 
        	 if(childIds.contains(tenureFacultyNode))
        	 {	
        		 found = true;
        		 System.out.println("found");
        	 }
        }
        return found;
        
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

            System.out.println(user.getName());
            // get all of the nodes accessible for the current user
            Map<Long, Set<String>> accessibleNodes = decider.getAccessibleNodes(user.getID(),123);
            for(long objectID : accessibleNodes.keySet()) {
                Node obj = graph.getNode(objectID);
                System.out.println("\t" + obj.getName() + "(" +obj.getType().toString()+") -> " + accessibleNodes.get(objectID));
            }
        }
        System.out.println("############### End Access state for " + step + "############");
    }
    
    
    private static void printAccessStateForUA(String step, Graph graph) throws PMException {
        System.out.println("############### Access state for " + step + " ###############");

        // initialize a PReviewDecider to make decisions
        PReviewDecider decider = new PReviewDecider(graph);

        // get all of the users in the graph
        Set<Node> search = graph.search(null, UA.toString(), null);
        for(Node user : search) {
            // there is a super user that we'll ignore
            if(user.getName().equals("super")) {
                continue;
            }

            System.out.println(user.getName());
            // get all of the nodes accessible for the current user
            Map<Long, Set<String>> accessibleNodes = decider.getAccessibleNodes(user.getID(),123);
            for(long objectID : accessibleNodes.keySet()) {
                Node obj = graph.getNode(objectID);
                System.out.println("\t" + obj.getName() + " -> " + accessibleNodes.get(objectID));
            }
        }
        System.out.println("############### End Access state for " + step + "############");
    }

    /**
     * Method to simulate an obligation. All obligations used in this example are triggered by an "assign to" event,
     * so we'll assume there is a child node being assigned to a target node.
     * @param graph the graph
     * @param userID the ID of the user that triggered the event
     * @param targetNode the node that the event happens on
     * @throws PMException
     */
    private static void simulateAssignToEvent(Graph graph, long userID, Node targetNode, Node childNode) throws PMException {
        // check if the target of the event is a particular container and execute the corresponding "response"
        if(targetNode.getID() == getNodeID(graph, "org_PDSs", OA, null)) {
            Obligations.createPDSNew(graph, userID, childNode);
        } else if(targetNode.getID() == getNodeID(graph, "CoPI", OA, null)) {
            Obligations.addCoPI(graph, childNode);
        } else if(targetNode.getID() == getNodeID(graph, "SP", OA, null)) {
            Obligations.addSP(graph, childNode);
        } else if(targetNode.getID() == getNodeID(graph, "submitted_pdss", OA, null)) {
            Obligations.submitPDS(graph, childNode);
        } else if(targetNode.getID() == getNodeID(graph, "cs_chair_approval", OA, null) ||
                targetNode.getID() == getNodeID(graph, "math_chair_approval", OA, null)) {
            Obligations.chairApproval(graph, childNode);
        }  else if(targetNode.getID() == getNodeID(graph, "coen_dean_approval", OA, null) ||
                targetNode.getID() == getNodeID(graph, "coas_dean_approval", OA, null)) {
            Obligations.deanApproval(graph, childNode);
        }
    }

    /**
     * Utility function to get the ID of a node iven it's name, type, and any properties it may have.
     */
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
    
    private static File getFileFromResources(PDS pds,String fileName) {
        ClassLoader classLoader = pds.getClass().getClassLoader();

        URL resource = classLoader.getResource(fileName);
        if (resource == null) {
            throw new IllegalArgumentException("file is not found!");
        } else {
            return new File(resource.getFile());
        }

    }
}
