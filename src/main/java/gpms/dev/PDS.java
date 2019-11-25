package gpms.dev;

import gov.nist.csd.pm.epp.events.AssignToEvent;
import gov.nist.csd.pm.epp.events.DeassignFromEvent;
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
import gov.nist.csd.pm.pip.obligations.model.Target;
import gov.nist.csd.pm.pip.obligations.model.actions.Action;
import gov.nist.csd.pm.pip.obligations.model.actions.CreateAction;
import gov.nist.csd.pm.pip.obligations.model.functions.Arg;
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
import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.PC;

import gpms.DAL.MongoDBConnector;
import gpms.dao.ProposalDAO;
import gpms.dao.UserAccountDAO;
import gpms.dao.UserProfileDAO;
import gpms.model.GPMSCommonInfo;
import gpms.model.UserAccount;
import gpms.model.UserInfo;
import gpms.model.UserProfile;
import gpms.ngac.policy.Attribute;
import gpms.ngac.policy.Constants;
import gpms.ngac.policy.NGACPolicyConfigurationLoader;
import gpms.ngac.policy.PDSOperations;
import gpms.ngac.policy.UserPermissionChecker;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.mongodb.morphia.Morphia;

import com.mongodb.MongoClient;



public class PDS {

    public static Random rand = new Random();
    

    public static void main(String[] args) throws IOException, PMException {
        // load the initial configuration from json
    	
		PDS main = new PDS();		
		//main.testGraph27WithObligations(main);
		//main.testGraph25WithObligations(main);
		//main.testGraph26WithProhibitions(main);
		
//		main.testMongoInfo();
//		main.testDeleteAllChildren(main);
//		main.testGenerateIds();
		main.testGenerateIds();
		

    }
    
    void testGenerateIds() {
    	System.out.println(getID());
    	System.out.println(getID());
    	System.out.println(getID());
    	System.out.println(getID());
    	System.out.println(getID());
    }
    
    void testMongoInfo() {
    	MongoClient mongoClient = null;
    	Morphia morphia = null;
    	String dbName = "db_gpms";
    	UserAccountDAO userAccountDAO = null;
    	UserProfileDAO userProfileDAO = null;
    	ProposalDAO proposalDAO = null;
    	mongoClient = MongoDBConnector.getMongo();
		morphia = new Morphia();
		morphia.map(UserProfile.class).map(UserAccount.class);
		userAccountDAO = new UserAccountDAO(mongoClient, morphia, dbName);
		userProfileDAO = new UserProfileDAO(mongoClient, morphia, dbName);
		proposalDAO = new ProposalDAO(mongoClient, morphia, dbName);
    	
		GPMSCommonInfo userInfo = new GPMSCommonInfo();
		//userInfo.setUserProfileID("5cddc20d2edd2f0d3c61c120");
		//userInfo.setUserName("nazmul");
		userInfo.setUserIsActive(true);
		//userInfo.setUserIsAdmin(false);
		//userInfo.setUserCollege("Engineering");
		//userInfo.setUserDepartment("Computer Science");
		//userInfo.setUserPositionType("Tenured/tenure-track faculty");
		//userInfo.setUserPositionTitle("Assistant Professor");
		try {
	    	System.out.println("User Info:"+userInfo.toString());
			//List<UserInfo> userList = userProfileDAO.findAllForAdminUserGrid(0, 1000, userInfo);
			List<UserProfile> userList = userProfileDAO.findAllUsersWithPosition();
			System.out.println("All positions:");
			for(UserProfile user : userList) {
				System.out.println(user.toString());
			}
			
			List<UserProfile> deptUsers = userProfileDAO.findAllForAdminUserGrid("DEPT");
			System.out.println("All Admins:");
			for(UserProfile user : deptUsers) {
				System.out.println(user.getUserAccount().getUserName()+" |"+user.getDetails(0).getDepartment()+"|"+user.getDetails(0).getCollege()+"|"+user.getDetails(0).getPositionTitle());
			}
			
			List<UserProfile> users = userProfileDAO.findAllForAdminUserGrid("UNIVERSITY");
			System.out.println("All Admins:");
			for(UserProfile user : users) {
				System.out.println(user.getUserAccount().getUserName()+" |"+user.getDetails(0).getDepartment()+"|"+user.getDetails(0).getCollege()+"|"+user.getDetails(0).getPositionTitle());
			}
			
			
		}catch(Exception e) {
			e.printStackTrace();
		}
		
    }
    
    void testDeleteAllChildren(PDS main) throws PMException, IOException {
    	File file = main.getFileFromResources(main, "docs/super_config.json");
		File file2 = main.getFileFromResources(main, "docs/proposal_creation.json");
		File file3 = main.getFileFromResources(main, "docs/university_organization.json");
		File file4 = main.getFileFromResources(main, "docs/editing_policyUp.json");
		
		
		File file5 = main.getFileFromResources(main, "docs/gpms_obligations.yml");

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
	        System.out.println("Graph:"+graph.toString());
        } catch(Exception e)
        {
        	System.out.println(e.toString());
        	e.printStackTrace();
        }
        
        Node node = graph.createNode(getID(), "nazmul", NodeType.U, null);
    	long  userAttNodeID = getNodeID(graph, "PI", NodeType.UA, null);
    	graph.assign(node.getID(), userAttNodeID);
    	Set<Long> childIds = graph.getChildren(userAttNodeID);
    	
    	System.out.println(childIds.size());
    	
    	for(long id : childIds) {
    		graph.deassign(id, userAttNodeID);
    	}
    	
    	childIds = graph.getChildren(userAttNodeID);
    	
    	System.out.println(childIds.size());
    	System.out.println(getNodeID(graph, "PI", NodeType.UA, null));
    	
    }
    
    void testGraph24WithProhibitions(PDS main) throws PMException, IOException {

		File file = main.getFileFromResources(main,"docs/test_prohibition.json"); 
		String json = new String(Files.readAllBytes(Paths.get(file.getAbsolutePath())));
		//System.out.println(json);
		Graph graph =null;
	    graph = GraphSerializer.fromJson(new MemGraph(), json);
		
		
		Prohibitions prohibitions = new MemProhibitions();

		
		File file_prohibition = main.getFileFromResources(main,"docs/prohibitions.json"); 
		String json_prohibition = new String(Files.readAllBytes(Paths.get(file_prohibition.getAbsolutePath())));
		prohibitions = ProhibitionsSerializer.fromJson(prohibitions, json_prohibition);
		
		String prohibitionString = ProhibitionsSerializer.toJson(prohibitions);	 	
	 	System.out.println("Prohibition:"+json_prohibition);

		PReviewDecider decider = new PReviewDecider(graph, prohibitions);
		Set<String> list = decider.list(2, 0, 6);
		
		for(String st: list) {
			System.out.print(st + " ");
		}
		System.out.println();
		Set<String> list2 = decider.list(2, 1, 7);
		
		for(String st2: list2) {
			System.out.print(st2 + " ");
		}
		System.out.println();
		Set<String> list3 = decider.list(2, 2, 10);
		
		for(String st2: list3) {
			System.out.print(st2 + " ");
		}
		
		PReviewDecider decider2 = new PReviewDecider(graph, new MemProhibitions());
		
		System.out.println();
		Set<String> list4 = decider2.list(2, 5, 6);
		
		for(String st2: list4) {
			System.out.print(st2 + " ");
		}
		
		boolean hasPermission = decider.check(2, NGACPolicyConfigurationLoader.getID() ,10, new String[] {"execute"});
		System.out.println(hasPermission);
		
		System.out.println(ProhibitionsSerializer.toJson(prohibitions));
	
	}
	
	
	
	void testGraph26WithProhibitions(PDS main) throws PMException, IOException {

		File file = main.getFileFromResources(main, "docs/super_config.json");
		File file2 = main.getFileFromResources(main, "docs/proposal_creation.json");
		File file3 = main.getFileFromResources(main, "docs/university_organization.json");
		File file4 = main.getFileFromResources(main, "docs/editing_policyUp.json");
		
		
		File file5 = main.getFileFromResources(main, "docs/gpms_obligations.yml");

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
	        System.out.println("Graph:"+graph.toString());
        } catch(Exception e)
        {
        	System.out.println(e.toString());
        	e.printStackTrace();
        }
		
		
		Prohibitions prohibitions = new MemProhibitions();		
		String pro = ProhibitionsSerializer.toJson(prohibitions);
		System.out.println("Prohibition:"+pro);
		//String prohibitionString = ProhibitionsSerializer.toJson(prohibitions);	 	
	 	
		
		
		System.out.println("*******Without prohibition********");
		PReviewDecider decider = new PReviewDecider(graph, new MemProhibitions());
		Set<String> list = decider.list(780103731515376518l, 0, 1432074838181907682l);
		
		for(String st: list) {
			System.out.println(st);
		}
		
		Boolean hasPermission = decider.check(780103731515376518l, NGACPolicyConfigurationLoader.getID() ,1432074838181907682l, new String[] {"Delete","Save", "Submit"});
		System.out.println(hasPermission);
		

		File file_prohibition = main.getFileFromResources(main,"docs/post_submission_prohibitions.json"); 
		String json_prohibition = new String(Files.readAllBytes(Paths.get(file_prohibition.getAbsolutePath())));
		prohibitions = ProhibitionsSerializer.fromJson(prohibitions, json_prohibition);
		
		//String prohibitionString = ProhibitionsSerializer.toJson(prohibitions);	 	
	 	System.out.println("Prohibition:"+json_prohibition);

		decider = new PReviewDecider(graph, prohibitions);
		list = decider.list(780103731515376518l, 1, 1432074838181907682l);
		
		for(String st: list) {
			System.out.println(st);
		}
		hasPermission = decider.check(780103731515376518l, NGACPolicyConfigurationLoader.getID() ,1432074838181907682l, new String[] {"Delete","Save","Submit"});
		System.out.println(hasPermission);
		
		
		
		
		//System.out.println(ProhibitionsSerializer.toJson(prohibitions));
		//assertEquals(1, list.size());
		//assertTrue(list.contains("execute"));
	}
	
    
    void testGraph25WithObligations(PDS main) throws PMException, IOException {

		File file = main.getFileFromResources(main,"docs/test_prohibition.json"); 
		String json = new String(Files.readAllBytes(Paths.get(file.getAbsolutePath())));
		//System.out.println(json);
		Graph graph =null;
	    graph = GraphSerializer.fromJson(new MemGraph(), json);
		
		
		Prohibitions prohibitions = new MemProhibitions();
		
		File file5 = main.getFileFromResources(main, "docs/test_obligation.yml");
		InputStream is = new FileInputStream(file5);
		Obligation obligation = EVRParser.parse(is); 
		
		PReviewDecider decider = new PReviewDecider(graph, prohibitions);
		Set<String> list = decider.list(2, 0, 7);
		
		for(String st: list) {
			System.out.print(st+" ");
		}
		System.out.println();
		boolean hasPermission = decider.check(2, NGACPolicyConfigurationLoader.getID() ,7, new String[] {"execute"});
		System.out.println(hasPermission);
		hasPermission = decider.check(2, NGACPolicyConfigurationLoader.getID() ,5, new String[] {"execute"});
		System.out.println(hasPermission);
		hasPermission = decider.check(2, NGACPolicyConfigurationLoader.getID() ,10, new String[] {"read"});
		System.out.println(hasPermission);
		hasPermission = decider.check(2, NGACPolicyConfigurationLoader.getID() ,10, new String[] {"delete"});
		System.out.println(hasPermission);
		
		PDP pdp = new PDP(new PAP(graph, new MemProhibitions(), new MemObligations()));
	 	pdp.getPAP().getObligationsPAP().add(obligation, true);

	 	
      
        long userID = getNodeID(graph, "u1", U, null);
        long childID = getNodeID(graph, "u2", U, null);
        long pdsSpUA = getNodeID(graph, "ua1", UA, null);
        pdp.getEPP().processEvent(new DeassignFromEvent(graph.getNode(pdsSpUA), graph.getNode(childID)),userID, 127);
		
        
		decider = new PReviewDecider(graph, prohibitions);
		list = decider.list(3, 1, 7);
		
		for(String st: list) {
			System.out.print(st+" ");
		}
		System.out.println();
		hasPermission = decider.check(2, NGACPolicyConfigurationLoader.getID() ,7, new String[] {"execute"});
		System.out.println(hasPermission);
		hasPermission = decider.check(2, NGACPolicyConfigurationLoader.getID() ,5, new String[] {"execute"});
		System.out.println(hasPermission);
		hasPermission = decider.check(2, NGACPolicyConfigurationLoader.getID() ,10, new String[] {"read"});
		System.out.println(hasPermission);
		hasPermission = decider.check(2, NGACPolicyConfigurationLoader.getID() ,10, new String[] {"delete"});
		System.out.println(hasPermission);
		
	}
    
    void testGraph27WithObligations(PDS main) throws PMException, IOException {

		File file = main.getFileFromResources(main, "docs/super_config.json");
		File file2 = main.getFileFromResources(main, "docs/proposal_creation.json");
		File file3 = main.getFileFromResources(main, "docs/university_organization.json");
		File file4 = main.getFileFromResources(main, "docs/editing_policyUp.json");
		
		
		//File file5 = main.getFileFromResources(main, "docs/gpms_obligations.yml");

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
	        System.out.println("Graph:"+graph.toString());
        } catch(Exception e)
        {
        	System.out.println(e.toString());
        	e.printStackTrace();
        }
        
        //nazmul -> Pi
        graph.assign(7890168025809137159l, 780103731515376518l);
        printAccessState("Initial configuration", graph);
		
		Prohibitions prohibitions = new MemProhibitions();

		File file5 = main.getFileFromResources(main, "docs/gpms_obligations.yml");
		InputStream is = new FileInputStream(file5);
		Obligation obligation = EVRParser.parse(is); 
		
		PReviewDecider decider = new PReviewDecider(graph, prohibitions);
		Set<String> list = decider.list(780103731515376518l, 0, 1432074838181907682l);
		
		System.out.println("PI->PI Editable Data");
		for(String st: list) {
			System.out.print(st+" ");
			}		
		System.out.println();
		System.out.println("PI->CoPI-Editable-Data");
		list = decider.list(780103731515376518l, 2, -125095002669469944l);		
		for(String st2: list) {
			System.out.print(st2+" ");
		}
		PDP pdp = new PDP(new PAP(graph, new MemProhibitions(), new MemObligations()));
	 	pdp.getPAP().getObligationsPAP().add(obligation, true);
	 	
      
        long userID = getNodeID(graph, "nazmul", U, null);
        long pdsSpUA = getNodeID(graph, "Submission-Info", OA, null);
        Node child = graph.createNode(5, "123", NodeType.O, null);//(graph, "ua1", UA, null);
        pdp.getEPP().processEvent(new AssignToEvent(graph.getNode(pdsSpUA), graph.getNode(child.getID())),userID, 129);
		
           
		System.out.println();
		list = decider.list(780103731515376518l, 2, 1432074838181907682l);		
		for(String str: list) {
			System.out.print(str+" ");
		}
		System.out.println();
		list = decider.list(780103731515376518l, 2, -125095002669469944l);		
		for(String str2: list) {
			System.out.print(str2+" ");
		}
		
		boolean hasPermission = decider.check(780103731515376518l, NGACPolicyConfigurationLoader.getID() ,-125095002669469944l, new String[] {"Delete"});
		System.out.println("Dec:"+hasPermission);
//		hasPermission = decider.check(2, NGACPolicyConfigurationLoader.getID() ,6, new String[] {"execute"});
//		System.out.println(hasPermission);
//		hasPermission = decider.check(2, NGACPolicyConfigurationLoader.getID() ,6, new String[] {"read"});
//		System.out.println(hasPermission);
		
//
//		PReviewDecider decider = new PReviewDecider(graph, prohibitions);
//		Set<String> list = decider.list(780103731515376518l, 0, 1432074838181907682l);
//		
//		for(String st: list) {
//			System.out.println(st);
//		}
//		boolean hasPermission = decider.check(780103731515376518l, NGACPolicyConfigurationLoader.getID() ,1432074838181907682l, new String[] {"Delete","Submit"});
//		System.out.println(hasPermission);
//		
//		System.out.println("***************");
//		decider = new PReviewDecider(graph, new MemProhibitions());
//		list = decider.list(780103731515376518l, 0, 1432074838181907682l);
//		
//		for(String st: list) {
//			System.out.println(st);
//		}
//		hasPermission = decider.check(780103731515376518l, NGACPolicyConfigurationLoader.getID() ,1432074838181907682l, new String[] {"Delete","Submit"});
//		System.out.println(hasPermission);
		
		//System.out.println(ProhibitionsSerializer.toJson(prohibitions));
		//assertEquals(1, list.size());
		//assertTrue(list.contains("execute"));
	}
    
    void testAll(PDS main) throws IOException,PMException {
    	
    	File file = main.getFileFromResources(main, "docs/super_config.json");
		File file2 = main.getFileFromResources(main, "docs/proposal_creation.json");
		File file3 = main.getFileFromResources(main, "docs/university_organization.json");
		File file4 = main.getFileFromResources(main, "docs/editing_policyUp.json");
		
		
		File file5 = main.getFileFromResources(main, "docs/gpms_obligations.yml");

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
	        System.out.println("Graph:"+graph.toString());
      //  printAccessState("Initial configuration", graph);
        } catch(Exception e)
        {
        	System.out.println(e.toString());
        	e.printStackTrace();
        }
        

		long randomId = getID(); 
		Node pdsNode = graph.createNode(randomId, ""+randomId, OA, null);
		
		long pdsOriginationOAID = getNodeID(graph, Constants.PDS_ORIGINATING_OA,  OA, null); 
		
		Obligation obligation=null;
		
		PDSOperations pdsOperations = new PDSOperations(); 
		GPMSCommonInfo userInfo = new GPMSCommonInfo();
		userInfo.setUserName("nazmul");		
		System.out.println(pdsOperations.hasPermissionToCreateAProposal(graph, userInfo, new MemProhibitions()));
		
		try{
    		InputStream is = new FileInputStream(file5);
    		obligation = EVRParser.parse(is);   		
    		
    		Prohibitions prohibitions = new MemProhibitions();
    		
    		//String name, Subject subject, List<Node> nodes, Set<String> operations, boolean intersection
//    		List<gov.nist.csd.pm.pip.prohibitions.model.Prohibition.Node> nodes = new ArrayList<gov.nist.csd.pm.pip.prohibitions.model.Prohibition.Node>();
//    		nodes.add(new gov.nist.csd.pm.pip.prohibitions.model.Prohibition.Node(1432074838181907682l, false));
//    		Subject sub = new Subject(780103731515376518l,Type.USER_ATTRIBUTE);
//    		Set<String> set = new HashSet<String>();
//    		set.add("w");
//    		Prohibition prohibition = new Prohibition("PI_deny_w",sub,nodes,set,true);
//    		
//    		prohibitions.add(prohibition);
//    		
//    		PDP pdp = new PDP(new PAP(graph, prohibitions, new MemObligations()));
//    	 	pdp.getPAP().getObligationsPAP().add(obligation, true);
//    	 	
//    	 	String prohibitionString = ProhibitionsSerializer.toJson(prohibitions);    	 	
//    	 	System.out.println("Prohibition:"+prohibitionString);
//    	 	
//    	 	PReviewDecider decider = new PReviewDecider(graph, prohibitions);
//    		Set<String> list = decider.list(780103731515376518l, 0, 1432074838181907682l);
//    		
//    		for(String st: list) {
//    			System.out.println(st);
//    		}
    	 	
    		PDP pdp = new PDP(new PAP(graph, new MemProhibitions(), new MemObligations()));
    	 	pdp.getPAP().getObligationsPAP().add(obligation, true);

	       // test u1 assign to
	        Set<Node> search = pdp.getPAP().getGraphPAP().search("nazmul", "O", null);	        
	        System.out.println("Found nazmul inside:"+ !search.isEmpty());
	        
	        long userID = getNodeID(graph, "nazmul", U, null);
	        pdp.getEPP().processEvent(new AssignToEvent(graph.getNode(pdsOriginationOAID), pdsNode),userID, 123);
		
	        
	        search = pdp.getPAP().getGraphPAP().search("nazmul", "O", null);	        
	        System.out.println("Found nazmul inside:"+ !search.isEmpty());
	        
	        printAccessState("Initial configuration", graph);
	        
	        //add Co PI : alice
	        
	        search = pdp.getPAP().getGraphPAP().search("liliana", "O", null);	        
	        System.out.println("Found liliana inside:"+ !search.isEmpty());
	        System.out.println(UserPermissionChecker.checkPermissionAnyType(graph, new MemProhibitions(), "liliana", U.toString(), new Attribute("CoPI-Editable-Data",NodeType.OA), Arrays.asList("w") ));
	        
	        long pdCoPIUA = getNodeID(graph, Constants.CO_PI_UA_LBL,  UA, null); 
			
	        long userIDAlice = getNodeID(graph, "liliana", U, null);
	        pdp.getEPP().processEvent(new AssignToEvent(graph.getNode(pdCoPIUA), graph.getNode(userIDAlice)),userID, 125);
		
	        
	        search = pdp.getPAP().getGraphPAP().search("liliana", "O", null);	        
	        System.out.println("Found alice inside:"+ !search.isEmpty());
	        
	        System.out.println(UserPermissionChecker.checkPermissionAnyType(graph, new MemProhibitions(), "liliana", U.toString(), new Attribute("CoPI-Editable-Data",NodeType.OA), Arrays.asList("w") ));
	        
	        
	        
	       // userInfo.setUserName("nazmul");		
		//	System.out.println("alice:"+pdsOperations.hasPermissionToAddAsCoPI(graph, userInfo, "alice"));
	        
	        
	        //add SP : dave
	        
	        
	        search = pdp.getPAP().getGraphPAP().search("tomtom", "O", null);	        
	        System.out.println("Found tomtom inside:"+ !search.isEmpty());
	        
	        long pdsSpUA = getNodeID(graph, Constants.SENIOR_PERSON_UA_LBL,  UA, null); 
			
	        long userIDDave = getNodeID(graph, "tomtom", U, null);
	        pdp.getEPP().processEvent(new AssignToEvent(graph.getNode(pdsSpUA), graph.getNode(userIDDave)),userID, 126);
		
	        
	        search = pdp.getPAP().getGraphPAP().search("tomtom", "O", null);	        
	        System.out.println("Found tomtom inside:"+ !search.isEmpty());
	        
	        search = pdp.getPAP().getGraphPAP().search("amy", "U", null);	        
	        System.out.println("Found amy inside:"+ !search.isEmpty());
	        
	        //Tenured Faculty
	        
	        
	        // delete sp:
	        
	        
	        //long pdsSpUA = getNodeID(graph, Constants.SENIOR_PERSON_OA_LBL,  UA, null); 
			
	       // long userIDDave = getNodeID(graph, "dave", U, null);
	        pdp.getEPP().processEvent(new DeassignFromEvent(graph.getNode(pdsSpUA), graph.getNode(userIDDave)),userID, 127);
		
	        
	        search = pdp.getPAP().getGraphPAP().search("tomtom", "O", null);	        
	        System.out.println("Found tomtom inside:"+ !search.isEmpty());
	        
	        search = pdp.getPAP().getGraphPAP().search("amy", "U", null);	        
	        System.out.println("Found amy inside:"+ !search.isEmpty());
	        
	        
	        
	        Node node = graph.createNode(randomId, ""+randomId, O, null);
	        long subInfoOA = getNodeID(graph, "Submission-Info",  OA, null); 
			
	        //long userIDDave = getNodeID(graph, "dave", U, null);
	        pdp.getEPP().processEvent(new AssignToEvent(graph.getNode(subInfoOA), node),userID, 128);
		
	        
	        
	        printAccessState("Initial configuration", graph);
	        
	        
	        
	        
		} catch(Exception e) {
			//System.out.println(e.toString());
			e.printStackTrace();
		}
    	
    }
    
    
    private static String createProposalId(long id)
	{
		return "PDS"+id;
	}
    
    
    
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
    
    
    private static void printGraph(Graph graph) throws PMException {
    	List<Node> nodes =  (List<Node>) graph.getNodes();
    	System.out.println("***********Nodes:************");
    	for(Node node: nodes) {
    		System.out.println(node.getName());
    	}
    	
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
    
	void testGraph23WithProhibitions() throws PMException {
		Graph graph = new MemGraph();

		Node u1 = graph.createNode(getID(), "u1", U, null);
		Node ua1 = graph.createNode(getID(), "ua1", UA, null);
		Node o1 = graph.createNode(getID(), "o1", NodeType.O, null);
		Node oa1 = graph.createNode(getID(), "oa1", OA, null);
		Node oa2 = graph.createNode(getID(), "oa2", OA, null);
		Node oa3 = graph.createNode(getID(), "oa3", OA, null);
		Node pc1 = graph.createNode(getID(), "pc1", PC, null);

		graph.assign(u1.getID(), ua1.getID());
		graph.assign(oa2.getID(), oa3.getID());
		graph.assign(o1.getID(), oa1.getID());
		graph.assign(o1.getID(), oa2.getID());
		graph.assign(oa3.getID(), pc1.getID());

		graph.associate(ua1.getID(), oa3.getID(), new HashSet<>(Arrays.asList("read", "write", "execute")));

		Prohibitions prohibitions = new MemProhibitions();
		Prohibition prohibition = new Prohibition();
		prohibition.setName("deny");
		prohibition.setSubject(new Prohibition.Subject(ua1.getID(), Prohibition.Subject.Type.USER_ATTRIBUTE));
		prohibition.setOperations(new HashSet<>(Arrays.asList("read")));
		//prohibition.addNode(new Prohibition.Node(oa1.getID(), false));
		prohibition.addNode(new Prohibition.Node(oa2.getID(), false));
		prohibition.setIntersection(true);
		prohibitions.add(prohibition);

//		prohibition = new Prohibition("deny2", new Prohibition.Subject(u1.getID(), Prohibition.Subject.Type.USER));
//		prohibition.setOperations(new HashSet<>(Arrays.asList("write")));
//		prohibition.addNode(new Prohibition.Node(oa3.getID(), false));
//		prohibition.setIntersection(true);
//		prohibitions.add(prohibition);
		
		String prohibitionString = ProhibitionsSerializer.toJson(prohibitions);
	 	
	 	System.out.println("Prohibition:"+prohibitionString);

		PReviewDecider decider = new PReviewDecider(graph, prohibitions);
		Set<String> list = decider.list(ua1.getID(), 0, oa2.getID());
		
		for(String st: list) {
			System.out.println(st);
		}
		//assertEquals(1, list.size());
		//assertTrue(list.contains("execute"));
	}
	
	
	
	
	
}
