package gpms.ngac.policy;

import gov.nist.csd.pm.epp.events.AssignToEvent;
import gov.nist.csd.pm.epp.events.DeassignFromEvent;
import gov.nist.csd.pm.epp.functions.FunctionExecutor;
import gov.nist.csd.pm.exceptions.PMException;
import gov.nist.csd.pm.operations.OperationSet;
import gov.nist.csd.pm.pap.PAP;
import gov.nist.csd.pm.pdp.PDP;
import gov.nist.csd.pm.pdp.decider.Decider;
import gov.nist.csd.pm.pdp.decider.PReviewDecider;
import gov.nist.csd.pm.pip.graph.Graph;
import gov.nist.csd.pm.pip.graph.GraphSerializer;
import gov.nist.csd.pm.pip.graph.MemGraph;
import gov.nist.csd.pm.pip.graph.dag.searcher.DepthFirstSearcher;
import gov.nist.csd.pm.pip.graph.dag.searcher.Direction;
import gov.nist.csd.pm.pip.graph.dag.visitor.Visitor;
import gov.nist.csd.pm.pip.graph.model.nodes.Node;
import gov.nist.csd.pm.pip.graph.model.nodes.NodeType;
import gov.nist.csd.pm.pip.obligations.MemObligations;
import gov.nist.csd.pm.pip.obligations.evr.EVRException;
import gov.nist.csd.pm.pip.obligations.evr.EVRParser;
import gov.nist.csd.pm.pip.obligations.model.Obligation;
import gov.nist.csd.pm.pip.obligations.model.functions.Function;
import gov.nist.csd.pm.pip.prohibitions.MemProhibitions;
import gov.nist.csd.pm.pip.prohibitions.Prohibitions;
import gov.nist.csd.pm.pip.prohibitions.ProhibitionsSerializer;
import gov.nist.csd.pm.pip.prohibitions.model.Prohibition;

import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.O;
import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.OA;
import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.U;
import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.UA;
import static gov.nist.csd.pm.pip.graph.model.nodes.Properties.NAMESPACE_PROPERTY;

import gov.nist.csd.pm.epp.EPPOptions;

import gpms.DAL.DepartmentsPositionsCollection;
import gpms.model.GPMSCommonInfo;
import gpms.model.InvestigatorInfo;
import gpms.ngac.policy.customEvents.AddCoPIEvent;
import gpms.ngac.policy.customEvents.AddSPEvent;
import gpms.ngac.policy.customEvents.ApproveEvent;
import gpms.ngac.policy.customEvents.CreateEvent;
import gpms.ngac.policy.customEvents.DeleteCoPIEvent;
import gpms.ngac.policy.customEvents.DeleteSPEvent;
import gpms.ngac.policy.customEvents.DisapproveEvent;
import gpms.ngac.policy.customEvents.SubmitEvent;
import gpms.ngac.policy.customEvents.SubmitRAEvent;
import gpms.ngac.policy.customFunctions.IsNodeInListExecutor;
import gpms.ngac.policy.customFunctions.RemovePropertyFromChildrenExecutor;
import gpms.ngac.policy.customFunctions.SPToAddExecutor;
import gpms.ngac.policy.customFunctions.SPToDeleteExecutor;
import gpms.ngac.policy.customFunctions.AddPropertiesToNodeExecutor;
import gpms.ngac.policy.customFunctions.AllChildrenHavePropertiesExecutor;
import gpms.ngac.policy.customFunctions.ChairForExecutor;
import gpms.ngac.policy.customFunctions.ChairsForExecutor;
import gpms.ngac.policy.customFunctions.CoPIToAddExecutor;
import gpms.ngac.policy.customFunctions.CoPIToDeleteExecutor;
import gpms.ngac.policy.customFunctions.CompareNodeNamesExecutor;
import gpms.ngac.policy.customFunctions.ConcatExecutor;
import gpms.ngac.policy.customFunctions.CreateNodeExecutor1;
import gpms.ngac.policy.customFunctions.DeanForExecutor;
import gpms.ngac.policy.customFunctions.DeleteNodeExecutor;
import gpms.ngac.policy.customFunctions.EmailExecutor;
import gpms.ngac.policy.customFunctions.GetAncestorInPCExecutor;
import gpms.ngac.policy.customFunctions.GetAncestorsInPCExecutor;
import gpms.ngac.policy.customFunctions.GetChildExecutor;
import gpms.ngac.policy.customFunctions.GetChildInPCExecutor;
import gpms.ngac.policy.customFunctions.GetChildrenUsersInPolicyClassExecutor;
import gpms.ngac.policy.customFunctions.HasChildrenExecutor;
import gpms.ngac.policy.customFunctions.IRBApprovalRequired;
import gpms.rest.UserService;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import java.util.Stack;

import org.apache.log4j.Logger;

/**
 * @author Md Nazmul Karim
 * @since May 20 2019
 * 
 *        This class is used to operate different functions over NGAC policy
 */
public class PDSOperations {

	private Graph ngacPolicy;
	public static Random rand = new Random();

	// private GpmsNgacObligations gpmsNgacObligations;

	public static HashMap<Long, Graph> proposalPolicies = new HashMap<Long, Graph>();
	public static HashMap<Long, Prohibitions> proposalProhibitions = new HashMap<Long, Prohibitions>();

	// public static Prohibitions proposalProhibitions = null;

	private static final Logger log = Logger.getLogger(PDSOperations.class.getName());

	private static NGACPolicyConfigurationLoader policyLoader;

	static Obligation obligation = null;
	static PDP pdp = null;

	public PDSOperations() {
		this.ngacPolicy = NGACPolicyConfigurationLoader.getPolicy();
		// gpmsNgacObligations = new GpmsNgacObligations();
		policyLoader = new NGACPolicyConfigurationLoader();

	}

	public PDSOperations(Graph gf) {
		this.ngacPolicy = gf;
		// gpmsNgacObligations = new GpmsNgacObligations();
	}

	public static PDP getPDP(Graph graph) throws PMException {
		DeleteNodeExecutor deleteNodeExecutor = new DeleteNodeExecutor();
		EmailExecutor emailExecutor = new EmailExecutor();
		IsAllowedToBeCoPIExecutor isAllowedToBeCoPIExecutor = new IsAllowedToBeCoPIExecutor();
		ChairForExecutor chairForExecutor = new ChairForExecutor();
		DeanForExecutor deanForExecutor = new DeanForExecutor();
		// BMForExecutor bmForExecutor = new BMForExecutor();
		CreateNodeExecutor1 createNodeExecutor1 = new CreateNodeExecutor1();
		ConcatExecutor concatExecutor = new ConcatExecutor();
		ChairsForExecutor chairsForExecutor = new ChairsForExecutor();
		IsNodeInListExecutor areSomeNodesContainedInExecutor = new IsNodeInListExecutor();
		CompareNodeNamesExecutor compareNodesExecutor = new CompareNodeNamesExecutor();
		CoPIToAddExecutor coPIToAddExecutor = new CoPIToAddExecutor();
		SPToAddExecutor spToAddExecutor = new SPToAddExecutor();
		CoPIToDeleteExecutor coPIToDeleteExecutor = new CoPIToDeleteExecutor();
		SPToDeleteExecutor spToDeleteExecutor = new SPToDeleteExecutor();
		AddPropertiesToNodeExecutor addPropertiesToNodeExecutor = new AddPropertiesToNodeExecutor();
		RemovePropertyFromChildrenExecutor removePropertiesFromChildrenExecutor = new RemovePropertyFromChildrenExecutor();
		AllChildrenHavePropertiesExecutor allChildrenHavePropertiesExecutor = new AllChildrenHavePropertiesExecutor();
		HasChildrenExecutor hasChildrenExecutor = new HasChildrenExecutor();
		IRBApprovalRequired iRBApprovalRequired = new IRBApprovalRequired();
		GetAncestorInPCExecutor getAncestorInPCExecutor = new GetAncestorInPCExecutor();
		GetChildInPCExecutor getChildInPCExecutor = new GetChildInPCExecutor();
		GetChildrenUsersInPolicyClassExecutor getChildrenInPCExecutor = new GetChildrenUsersInPolicyClassExecutor();
		GetChildExecutor getChildExecutor = new GetChildExecutor();
		GetAncestorsInPCExecutor getAncestorsInPCExecutor = new GetAncestorsInPCExecutor();

		obligation = policyLoader.getObligation();
		EPPOptions eppOptions = new EPPOptions(deleteNodeExecutor, emailExecutor, chairForExecutor, deanForExecutor,
				isAllowedToBeCoPIExecutor, createNodeExecutor1, concatExecutor, chairsForExecutor,
				areSomeNodesContainedInExecutor, compareNodesExecutor, coPIToAddExecutor, spToAddExecutor,
				coPIToDeleteExecutor, spToDeleteExecutor, addPropertiesToNodeExecutor,
				removePropertiesFromChildrenExecutor, allChildrenHavePropertiesExecutor, hasChildrenExecutor,
				iRBApprovalRequired, getAncestorInPCExecutor, getChildInPCExecutor, getChildrenInPCExecutor,
				getChildExecutor, getAncestorsInPCExecutor);
		pdp = new PDP(new PAP(graph, new MemProhibitions(), new MemObligations()), eppOptions);
		pdp.getPAP().getObligationsPAP().add(obligation, true);

		return pdp;
	}

	public Graph getNGACPolicy() {
		return this.ngacPolicy;
	}

	public Graph getBacicNGACPolicy() {
		return policyLoader.reloadBasicConfig();
	}

	/**
	 * This function checks whether a user has permission for a task
	 * 
	 * @param userName
	 * @return true/false
	 */
	public boolean hasPermissionToCreateAProposal(String userName, Prohibitions prohibitions) {
		boolean hasPermission = true;

		HashMap map = Task.CREATE_PROPOSAL.getPermissionsSets();

		Iterator<Map.Entry<Attribute, HashSet>> itr = map.entrySet().iterator();

		while (itr.hasNext()) {
			Map.Entry<Attribute, HashSet> entry = itr.next();
			log.info("Container = " + entry.getKey() + ", permission set = " + entry.getValue());
			hasPermission = UserPermissionChecker.checkPermissionAnyType(ngacPolicy, prohibitions, userName,
					U.toString(), (Attribute) entry.getKey(), new ArrayList<String>(entry.getValue()));
		}

		log.info("Create Proposal Permission : " + hasPermission);

		return hasPermission;
	}

	public boolean hasPermissionToCreateAProposal(Graph policy, GPMSCommonInfo userInfo, Prohibitions prohibitions) {
		boolean hasPermission = true;

		HashMap map = Task.CREATE_PROPOSAL.getPermissionsSets();

		Iterator<Map.Entry<Attribute, HashSet>> itr = map.entrySet().iterator();

		while (itr.hasNext()) {
			Map.Entry<Attribute, HashSet> entry = itr.next();
			log.info("Container = " + entry.getKey() + ", permission set = " + entry.getValue());
			hasPermission = UserPermissionChecker.checkPermissionAnyType(policy, prohibitions, userInfo.getUserName(),
					U.toString(), (Attribute) entry.getKey(), new ArrayList<String>(entry.getValue()));
		}

		log.info("Create Proposal Permission : " + hasPermission);

		return hasPermission;
	}

	/**
	 * This function checks whether a user has permission to add another user as
	 * CoPI
	 * 
	 * @param userName             the performer
	 * @param coPIApproachableUser the intended user to be a CoPI
	 * @return true/false
	 */

	public boolean hasPermissionToAddAsCoPI(Graph policy, String userName, String coPIApproachableUser,
			Prohibitions prohibitions) {
		boolean hasPermission = true;
		//log.info("Graph Policy:"+ GraphSerializer.toJson(ngacPolicy));
		String[] requiredAccessRights = new String[1];
		requiredAccessRights[0] = "add-copi";
		PReviewDecider decider = new PReviewDecider(policy,prohibitions);
		HashMap map = Task.ADD_CO_PI.getPermissionsSets();
		Iterator<Map.Entry<Attribute, HashSet>> itr = map.entrySet().iterator();

		while (itr.hasNext()) {
			Map.Entry<Attribute, HashSet> entry = itr.next();
			log.info("Container = " + entry.getKey() + ", permission set = " + entry.getValue());
			System.out.println("Container = " + entry.getKey() + ", permission set = " + entry.getValue());
			try {
				hasPermission = hasPermission && decider.check(userName, "process" ,"CoPI", requiredAccessRights);
			} catch (PMException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		// try {
		// hasPermission = hasPermission && isChildrenFound(policy,
		// coPIApproachableUser, Constants.CO_PI_UA_LBL);
		// }
		// catch(PMException e){
		// e.printStackTrace();
		// }
		log.info("Add CoPI Permission : " + hasPermission);
		System.out.println("Add CoPI Permission : " + hasPermission);

		return hasPermission;
	}

	public boolean hasPermissionToDeleteCoPI(Graph policy, String userName, Prohibitions prohibitions) {
		boolean hasPermission = true;
		String[] requiredAccessRights = new String[1];
		requiredAccessRights[0] = "delete-copi";
		PReviewDecider decider = new PReviewDecider(policy,prohibitions);
		HashMap map = Task.DELETE_CO_PI.getPermissionsSets();

		Iterator<Map.Entry<Attribute, HashSet>> itr = map.entrySet().iterator();

		while (itr.hasNext()) {
			Map.Entry<Attribute, HashSet> entry = itr.next();
			log.info("Container = " + entry.getKey() + ", permission set = " + entry.getValue());
			System.out.println("Container = " + entry.getKey() + ", permission set = " + entry.getValue());
			try {
				hasPermission = hasPermission && decider.check(userName, "process" ,"CoPI", requiredAccessRights);
			} catch (PMException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
//        try {
//        	hasPermission = hasPermission && isChildrenFound(policy, coPIApproachableUser, Constants.CO_PI_UA_LBL);
//        }
//        catch(PMException e){
//        	e.printStackTrace();
//        }
		log.info("Delete CoPI Permission : " + hasPermission);
		System.out.println("Delete CoPI Permission : " + hasPermission);

		return hasPermission;
	}

	public boolean hasPermissionToDeleteSP(Graph policy, String userName, Prohibitions prohibitions) {
		boolean hasPermission = true;
		String[] requiredAccessRights = new String[1];
		requiredAccessRights[0] = "delete-sp";
		PReviewDecider decider = new PReviewDecider(policy,prohibitions);
		HashMap map = Task.DELETE_SP.getPermissionsSets();

		Iterator<Map.Entry<Attribute, HashSet>> itr = map.entrySet().iterator();

		while (itr.hasNext()) {
			Map.Entry<Attribute, HashSet> entry = itr.next();
			log.info("Container = " + entry.getKey() + ", permission set = " + entry.getValue());
			System.out.println("Container = " + entry.getKey() + ", permission set = " + entry.getValue());
			try {
				hasPermission = hasPermission && decider.check(userName, "process" ,"SP", requiredAccessRights);
			} catch (PMException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
//        try {
//        	hasPermission = hasPermission && isChildrenFound(policy, coPIApproachableUser, Constants.CO_PI_UA_LBL);
//        }
//        catch(PMException e){
//        	e.printStackTrace();
//        }
		log.info("Delete SP Permission : " + hasPermission);
		System.out.println("Delete SP Permission : " + hasPermission);

		return hasPermission;
	}

	public static Set<String> getElegibleUsers(String parent) throws PMException {
		Graph graph = null;

		graph = policyLoader.reloadBasicConfig();
		graph = policyLoader.createAProposalGraph(graph); // loads editing policy
		graph = policyLoader.createAprovalGraph(graph); // loads editing policy

		Node parentNode = graph.getNode(parent);
		DepthFirstSearcher dfs = new DepthFirstSearcher(graph);
		Set<String> nodes = new HashSet<>();
		Visitor visitor = node -> {
			if (node.getType().toString().equals("U"))
				nodes.add(node.getName());
		};
		dfs.traverse(parentNode, (c, p) -> {
		}, visitor, Direction.CHILDREN);

		return nodes;
	}

	public static List<String> getUserChildrenInGraph(String parent, Graph graph) throws PMException {
		List<String> listOfChildren = new ArrayList<String>();

		List<String> listOfChildrenUsers = new ArrayList<String>();

		listOfChildren.addAll(graph.getChildren(parent));

		for (String nodeName : listOfChildren) {
			if (graph.getNode(nodeName).getType().toString().equals("U")) {
				listOfChildrenUsers.add(nodeName);
				System.out.println("ADDED: " + nodeName);
			}
		}

		return listOfChildrenUsers;
	}

	public static List<String> getAcademicAdminUserAttributes() {

		List<String> listOfAdmins = new ArrayList<String>();

		listOfAdmins.add("Chair");
		listOfAdmins.add("Dean");
		listOfAdmins.add("Business-Manager");
		listOfAdmins.add("Research Admin");
		listOfAdmins.add("Research Director");

		return listOfAdmins;
	}

	public static List<String> getAdministrationAdminUsers() {

		List<String> listOfAdmins = new ArrayList<String>();
		listOfAdmins.add("irbglobal");
		return listOfAdmins;
	}

	public static boolean isContainedIn() {

		return false;
	}

	/**
	 * This function checks whether a user has permission to add another user as SP
	 * 
	 * @param userName           the performer
	 * @param spApproachableUser the intended user to be a SP
	 * @return true/false
	 * @throws PMException
	 */
	public boolean hasPermissionToAddAsSP(Graph policy, String userName, String spApproachableUser,
			Prohibitions prohibitions) {
		boolean hasPermission = true;

		HashMap map = Task.ADD_SP.getPermissionsSets();

		Iterator<Map.Entry<Attribute, HashSet>> itr = map.entrySet().iterator();

		while (itr.hasNext()) {
			Map.Entry<Attribute, HashSet> entry = itr.next();
			log.info("Container = " + entry.getKey() + ", permission set = " + entry.getValue());
			hasPermission = hasPermission && UserPermissionChecker.checkPermissionAnyType(policy, prohibitions, "CoPI",
					UA.toString(), (Attribute) entry.getKey(), new ArrayList<String>(entry.getValue()));
		}
//		try {
//			hasPermission = hasPermission
//					&& isChildrenFound(policy, spApproachableUser, Constants.SENIOtargetIdR_PERSON_UA_LBL);
//		} catch (PMException e) {
//			e.printStackTrace();
//		}
		log.info("Add SP Permission : " + hasPermission);

		return hasPermission;
	}

	public long createAProposal(String PI, String department, String email) throws PMException {

		long randomId = getID();

		try {
			String chairDept = DepartmentsPositionsCollection.adminUsers
					.get(DepartmentsPositionsCollection.departmentNames.get(department) + "CHAIR");
			String deanDept = DepartmentsPositionsCollection.adminUsers
					.get(DepartmentsPositionsCollection.departmentNames.get(department) + "DEAN");
			String bmDept = DepartmentsPositionsCollection.adminUsers
					.get(DepartmentsPositionsCollection.departmentNames.get(department) + "BM");
			Graph proposalPolicy = null;

			proposalPolicy = policyLoader.reloadBasicConfig();
			proposalPolicy = policyLoader.createAProposalGraph(proposalPolicy); // loads editing policy
			proposalPolicy = policyLoader.createAprovalGraph(proposalPolicy); // loads editing policy
			// Map<String, String> properties = new HashMap<String, String>();
			// properties.put("workEmail", email);
			// properties.put("departmentChair", chairDept);
			// properties.put("departmentDean", deanDept);
			// properties.put("departmentBM", bmDept);

			// proposalPolicy.updateNode(PI, properties);

			getPDP(proposalPolicy).getEPP()
					.processEvent(new CreateEvent(proposalPolicy.getNode(Constants.PDS_ORIGINATING_OA)), PI, "process");
			PDSOperations.proposalPolicies.put(randomId, proposalPolicy);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return randomId;
	}

	public PDP submitAProposal(String PI, String JSONGraph, Boolean irbApprovalRequired) throws PMException {
		Graph graph = new MemGraph();
		GraphSerializer.fromJson(graph, JSONGraph);
		if (graph.exists("super_pc_rep")) {
			graph.deleteNode("super_pc_rep");
		}
		PDP pdp = getPDP(graph);
		pdp.getEPP().processEvent(new SubmitEvent(graph.getNode(Constants.SUBMISSION_INFO_OA_LBL), irbApprovalRequired),
				PI, "process");
		return pdp;
	}

	public static void addCoPI(String PI, String CoPIUser, String CoPIUA, Graph intialGraph) throws Exception {
		if (intialGraph.exists("super_pc_rep")) {
			intialGraph.deleteNode("super_pc_rep");
		}
		PDP pdp = getPDP(intialGraph);
		pdp.getEPP().processEvent(new AddCoPIEvent(intialGraph.getNode(CoPIUA), intialGraph.getNode(CoPIUser)), PI,
				"process");

		if (!intialGraph.getParents(CoPIUser).contains("CoPI")) {
			throw new Exception();
		}
	}

	public static void addSP(String PIorCoPI, String SPUser, String SPUA, Graph intialGraph) throws Exception {
		if (intialGraph.exists("super_pc_rep")) {
			intialGraph.deleteNode("super_pc_rep");
		}
		PDP pdp = getPDP(intialGraph);
		pdp.getEPP().processEvent(new AddSPEvent(intialGraph.getNode(SPUA), intialGraph.getNode(SPUser)), PIorCoPI,
				"process");
		if (!intialGraph.getParents(SPUser).contains("SP")) {
			throw new Exception();
		}
	}

	public static void deleteCoPI(String PI, String CoPIUser, String CoPIUA, Graph intialGraph) throws PMException {
		if (intialGraph.exists("super_pc_rep")) {
			intialGraph.deleteNode("super_pc_rep");
		}
		PDP pdp = getPDP(intialGraph);
		try {
			System.out.println();
			pdp.getEPP().processEvent(new DeleteCoPIEvent(intialGraph.getNode(CoPIUA), intialGraph.getNode(CoPIUser)),
					PI, "process");
		} catch (NoSuchElementException ex) {
			ex.printStackTrace();
		}
	}

	public static void deleteSP(String PIorCoPI, String SPUser, String SPUA, Graph intialGraph) throws PMException {
		if (intialGraph.exists("super_pc_rep")) {
			intialGraph.deleteNode("super_pc_rep");
		}
		PDP pdp = getPDP(intialGraph);
		try {
			pdp.getEPP().processEvent(new DeleteSPEvent(intialGraph.getNode(SPUA), intialGraph.getNode(SPUser)),
					PIorCoPI, "process");
		} catch (NoSuchElementException ex) {
			ex.printStackTrace();
		}
	}

	public PDP chairApprove(String chair, String JSONGraph) throws PMException {
		Graph graph = new MemGraph();
		GraphSerializer.fromJson(graph, JSONGraph);
		if (graph.exists("super_pc_rep")) {
			graph.deleteNode("super_pc_rep");
		}
		PDP pdp = getPDP(graph);
		pdp.getEPP().processEvent(new ApproveEvent(graph.getNode(Constants.CHAIR_APPROVAL)), chair, "process");
		return pdp;
	}

	public PDP chairDisapprove(String chair, String JSONGraph) throws PMException {
		Graph graph = new MemGraph();
		GraphSerializer.fromJson(graph, JSONGraph);
		if (graph.exists("super_pc_rep")) {
			graph.deleteNode("super_pc_rep");
		}
		PDP pdp = getPDP(graph);
		pdp.getEPP().processEvent(new DisapproveEvent(graph.getNode(Constants.CHAIR_APPROVAL)), chair, "process");
		return pdp;
	}

	public PDP bmApprove(String bm, String JSONGraph) throws PMException {
		Graph graph = new MemGraph();
		GraphSerializer.fromJson(graph, JSONGraph);
		if (graph.exists("super_pc_rep")) {
			graph.deleteNode("super_pc_rep");
		}
		PDP pdp = getPDP(graph);
		pdp.getEPP().processEvent(new ApproveEvent(graph.getNode(Constants.BM_APPROVAL)), bm, "process");
		return pdp;
	}

	public PDP bmDisapprove(String bm, String JSONGraph) throws PMException {
		Graph graph = new MemGraph();
		GraphSerializer.fromJson(graph, JSONGraph);
		if (graph.exists("super_pc_rep")) {
			graph.deleteNode("super_pc_rep");
		}
		PDP pdp = getPDP(graph);
		pdp.getEPP().processEvent(new DisapproveEvent(graph.getNode(Constants.BM_APPROVAL)), bm, "process");
		return pdp;
	}

	public PDP deanApprove(String dean, String JSONGraph) throws PMException {
		Graph graph = new MemGraph();
		GraphSerializer.fromJson(graph, JSONGraph);
		if (graph.exists("super_pc_rep")) {
			graph.deleteNode("super_pc_rep");
		}
		PDP pdp = getPDP(graph);
		pdp.getEPP().processEvent(new ApproveEvent(graph.getNode(Constants.DEAN_APPROVAL)), dean, "process");
		return pdp;
	}

	public PDP deanDisapprove(String dean, String JSONGraph) throws PMException {
		Graph graph = new MemGraph();
		GraphSerializer.fromJson(graph, JSONGraph);
		if (graph.exists("super_pc_rep")) {
			graph.deleteNode("super_pc_rep");
		}
		PDP pdp = getPDP(graph);
		pdp.getEPP().processEvent(new DisapproveEvent(graph.getNode(Constants.DEAN_APPROVAL)), dean, "process");
		return pdp;
	}

	public PDP irbApprove(String irb, String JSONGraph) throws PMException {
		Graph graph = new MemGraph();
		GraphSerializer.fromJson(graph, JSONGraph);
		if (graph.exists("super_pc_rep")) {
			graph.deleteNode("super_pc_rep");
		}
		PDP pdp = getPDP(graph);
		pdp.getEPP().processEvent(new ApproveEvent(graph.getNode(Constants.IRB_APPROVAL)), irb, "process");
		return pdp;
	}

	public PDP irbDisapprove(String irb, String JSONGraph) throws PMException {
		Graph graph = new MemGraph();
		GraphSerializer.fromJson(graph, JSONGraph);
		if (graph.exists("super_pc_rep")) {
			graph.deleteNode("super_pc_rep");
		}
		PDP pdp = getPDP(graph);
		pdp.getEPP().processEvent(new DisapproveEvent(graph.getNode(Constants.IRB_APPROVAL)), irb, "process");
		return pdp;
	}

	public PDP raApprove(String ra, String JSONGraph) throws PMException {
		Graph graph = new MemGraph();
		GraphSerializer.fromJson(graph, JSONGraph);
		if (graph.exists("super_pc_rep")) {
			graph.deleteNode("super_pc_rep");
		}
		PDP pdp = getPDP(graph);
		pdp.getEPP().processEvent(new ApproveEvent(graph.getNode(Constants.RA_APPROVAL)), ra, "process");
		return pdp;
	}

	public PDP raDisapprove(String ra, String JSONGraph) throws PMException {
		Graph graph = new MemGraph();
		GraphSerializer.fromJson(graph, JSONGraph);
		if (graph.exists("super_pc_rep")) {
			graph.deleteNode("super_pc_rep");
		}
		PDP pdp = getPDP(graph);
		pdp.getEPP().processEvent(new DisapproveEvent(graph.getNode(Constants.RA_APPROVAL)), ra, "process");
		return pdp;
	}

	public PDP rdApprove(String rd, String JSONGraph) throws PMException {
		Graph graph = new MemGraph();
		GraphSerializer.fromJson(graph, JSONGraph);
		if (graph.exists("super_pc_rep")) {
			graph.deleteNode("super_pc_rep");
		}
		PDP pdp = getPDP(graph);
		pdp.getEPP().processEvent(new ApproveEvent(graph.getNode(Constants.RD_APPROVAL)), rd, "process");
		return pdp;
	}

	public PDP rdDisapprove(String rd, String JSONGraph) throws PMException {
		Graph graph = new MemGraph();
		GraphSerializer.fromJson(graph, JSONGraph);
		if (graph.exists("super_pc_rep")) {
			graph.deleteNode("super_pc_rep");
		}
		PDP pdp = getPDP(graph);
		pdp.getEPP().processEvent(new DisapproveEvent(graph.getNode(Constants.RD_APPROVAL)), rd, "process");
		return pdp;
	}

	public PDP raSubmit(String ra, String JSONGraph) throws PMException {
		Graph graph = new MemGraph();
		GraphSerializer.fromJson(graph, JSONGraph);
		if (graph.exists("super_pc_rep")) {
			graph.deleteNode("super_pc_rep");
		}
		PDP pdp = getPDP(graph);
		pdp.getEPP().processEvent(new SubmitRAEvent(graph.getNode(Constants.SUBMISSION_INFO_OA_LBL)), ra, "process");
		return pdp;
	}

	/**
	 * Utility method to print the current access state to the console.
	 * 
	 * @param step  the name of the step
	 * @param graph the graph to determine permissions
	 */
	public static void printAccessState(String step, Graph graph) throws PMException {
		System.out.println("############### Access state for " + step + " ###############");

		// initialize a PReviewDecider to make decisions
		PReviewDecider decider = new PReviewDecider(graph);

		// get all of the users in the graph
		Set<Node> search = graph.search(U, null);
		for (Node user : search) {
			// there is a super user that we'll ignore
			if (user.getName().equals("super")) {
				continue;
			}

			log.info(user.getName());
			// get all of the nodes accessible for the current user
//			Map<Long, Set<String>> accessibleNodes = decider.getAccessibleNodes(user.getName(), 100);
//			for (long objectID : accessibleNodes.keySet()) {
//				Node obj = graph.getNode(objectID);
//				log.info("\t" + obj.getName() + " -> " + accessibleNodes.get(objectID));
//			}
		}
		log.info("############### End Access state for " + step + "############");
	}

	public static void printGraph(Graph graph) throws PMException {
		List<Node> nodes = (List<Node>) graph.getNodes();
		System.out.println("***********Nodes:************");
		for (Node node : nodes) {
			System.out.println(node.getName());
		}

	}

	public static long getID() {
		return rand.nextLong();
	}

	public boolean doesPolicyBelongToNGAC(HashMap<String, String> attr) {
		if (attr.get("position.type") != null && attr.get("proposal.section").equalsIgnoreCase("Whole Proposal")
				&& attr.get("proposal.action").equalsIgnoreCase("Add"))
			return true;
		return false;
	}

	public boolean isChildrenFound(Graph policy, String name, String parent) throws PMException {
		boolean found = false;
		// get all of the users in the graph
		Node userAttNode = policy.getNode(parent);

		// System.out.println(search.size());

		// for (Node userAttNode : search) {

		Set<String> childIds = policy.getChildren(userAttNode.getName());
		log.info("No of Children Assigned on " + parent + " :" + childIds.size() + "|" + childIds);

		// long sourceNode = getNodeID(policy, name, U, null);

		log.info("We are looking for:" + name);

		if (childIds.contains(name)) {
			found = true;
			log.info("found");
		} else {
			log.info("not found");
		}
		// }
		return found;

	}
	public static boolean getAccessDecisionInJSONGraph(String jsonGraph, String subject, String accessRight, String target) {
		boolean result = false;
		
		Graph graph =new MemGraph();
		try {
			GraphSerializer.fromJson(graph, jsonGraph);
		} catch (Exception e) {
			return false;
		}
		
		PReviewDecider decider = new PReviewDecider(graph); 
		try {
			result = decider.check(subject, "", target, accessRight);
		} catch (Exception e) {
			return false;
		}
		
		return result; 
	}
	public void testUsersAccessights_Proposal_created(Graph proposalPolicy) {

		// long userIdNazmul = PDSOperations.getNodeID(proposalPolicy, "nazmul",
		// NodeType.U, null); // tanure track +
		// cs
		// long userIdAmy = PDSOperations.getNodeID(proposalPolicy, "amy", NodeType.U,
		// null); // adjunct
		// long userIdtom = PDSOperations.getNodeID(proposalPolicy, "tomtom",
		// NodeType.U, null); // adjunct
		// long userIdSamer = PDSOperations.getNodeID(proposalPolicy, "samer",
		// NodeType.U, null); // CE

		// long userIdCSChair = PDSOperations.getNodeID(proposalPolicy,
		// DepartmentsPositionsCollection.adminUsers.get("CSCHAIR"), NodeType.U, null);

		log.info("************Start**************");
		Attribute att = new Attribute("Budget-Info", NodeType.OA);
		String[] ops = new String[] { "w" };
		boolean hasPermission = UserPermissionChecker.checkPermissionAnyType(proposalPolicy, new MemProhibitions(),
				"nazmul", "U", att, Arrays.asList(ops));
		log.info("Nazmul:Budget-Info(w):" + hasPermission);

		att = new Attribute("Budget-Info", NodeType.OA);
		ops = new String[] { "w" };
		String userName = "tomtom";
		hasPermission = UserPermissionChecker.checkPermissionAnyType(proposalPolicy, new MemProhibitions(), userName,
				"U", att, Arrays.asList(ops));
		log.info("tomtom:Budget-Info(w):" + hasPermission);

		att = new Attribute("Project-Info", NodeType.OA);
		ops = new String[] { "r" };
		userName = "tomtom";
		hasPermission = UserPermissionChecker.checkPermissionAnyType(proposalPolicy, new MemProhibitions(), userName,
				"U", att, Arrays.asList(ops));
		log.info("Project-Info(w):" + hasPermission);
		log.info("**************End************");

	}

	public void testUsersAccessights_Proposal_not_created() {

		log.info("************Start**************");
		// long userIdNazmul = PDSOperations.getNodeID(ngacPolicy, "nazmul", NodeType.U,
		// null); // tanure track + cs
		// long userIdAmy = PDSOperations.getNodeID(ngacPolicy, "amy", NodeType.U,
		// null); // adjunct
		Attribute att = new Attribute("PDS", NodeType.OA);
		String[] ops = new String[] { "create-oa" };
		boolean hasPermission = UserPermissionChecker.checkPermissionAnyType(ngacPolicy, new MemProhibitions(),
				"nazmul", "U", att, Arrays.asList(ops));
		log.info("Nazmul:Create Proposal:" + hasPermission);
		hasPermission = UserPermissionChecker.checkPermissionAnyType(ngacPolicy, new MemProhibitions(), "amy", "U", att,
				Arrays.asList(ops));
		log.info("Amy:Create Proposal:" + hasPermission);
		log.info("************End**************");

	}

}
