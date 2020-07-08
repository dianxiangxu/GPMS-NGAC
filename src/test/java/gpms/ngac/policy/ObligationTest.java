package gpms.ngac.policy;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import gov.nist.csd.pm.epp.EPPOptions;
import gov.nist.csd.pm.exceptions.PMException;
import gov.nist.csd.pm.operations.OperationSet;
import gov.nist.csd.pm.pap.PAP;
import gov.nist.csd.pm.pdp.PDP;
import gov.nist.csd.pm.pdp.decider.PReviewDecider;
import gov.nist.csd.pm.pip.graph.Graph;
import gov.nist.csd.pm.pip.graph.GraphSerializer;
import gov.nist.csd.pm.pip.graph.MemGraph;
import gov.nist.csd.pm.pip.obligations.MemObligations;
import gov.nist.csd.pm.pip.obligations.evr.EVRParser;
import gov.nist.csd.pm.pip.obligations.model.Obligation;
import gov.nist.csd.pm.pip.prohibitions.MemProhibitions;
import gpms.ngac.policy.customEvents.*;
import gpms.ngac.policy.customFunctions.*;


public class ObligationTest {
	// Graph
	Graph ngacGraph = new MemGraph();
	// Obligations
	Obligation obligation;

	@Before
	public void setUp() throws Exception {
		// File for super policy
		File file_super = getFileFromResources(Constants.POLICY_CONFIG_FILE_SUPER);
		// File for eligibility policy
		File file_eligibility_policy = getFileFromResources(Constants.POLICY_CONFIG_ELIGIBILITY_POLICY);
		// File for organization policy
		File file_organization_policy = getFileFromResources(Constants.POLICY_CONFIG_ACADEMIC_UNITS_POLICY_CLASS);
		// File for editing policy
		File file_editing_policy = getFileFromResources(Constants.PDS_EDITING_TEMPLATE);
		// File for obligation policy
		File obligationFile = getFileFromResources(Constants.OBLIGATION_TEMPLATE_PROPOSAL_CREATION);
		// JSON string super policy
		String super_policy = new String(Files.readAllBytes(Paths.get(file_super.getAbsolutePath())));
		// JSON string eligibility policy
		String eligibility_policy = new String(
				Files.readAllBytes(Paths.get(file_eligibility_policy.getAbsolutePath())));
		// JSON string organization+administration policy
		String organization_policy = new String(
				Files.readAllBytes(Paths.get(file_organization_policy.getAbsolutePath())));
		// JSON string editing policy
		String editing_policy = new String(Files.readAllBytes(Paths.get(file_editing_policy.getAbsolutePath())));
		// Adding super policy to graph
		GraphSerializer.fromJson(ngacGraph, super_policy);
		// Adding eligibility policy to graph
		GraphSerializer.fromJson(ngacGraph, eligibility_policy);
		// Adding organization policy to graph policy to graph
		GraphSerializer.fromJson(ngacGraph, organization_policy);
		// Adding editing policy to graph
		GraphSerializer.fromJson(ngacGraph, editing_policy);
		// Getting obligations as input stream
		InputStream is = new FileInputStream(obligationFile);
		// Parsing the obligation input stream to Obligation object
		obligation = EVRParser.parse(is);

	}

//	@Test
//	public void createProposalTest() {
//		try {
//			// asserting that User nazmul is not currently assigned to PI User Attribute
//			assertFalse(ngacGraph.getChildren("PI").contains("nazmul"));
	
//			// asserting that User ChairCSUser is not currently assigned to Chair User
//			// Attribute
//			assertFalse(ngacGraph.getChildren("Chair").contains("ChairCSUser"));
//			// asserting that User bmCSUser is not currently assigned to Business Manager
//			// User Attribute
//			assertFalse(ngacGraph.getChildren("Business Manager").contains("bmCSUser"));
//			// asserting that User DeanCOEUser is not currently assigned to Dean User
//			// Attribute
//			assertFalse(ngacGraph.getChildren("Dean").contains("DeanCOEUser"));
//
//			// processing custom event "CreateEvent", passing "PDS" object attribute as a
//			// target of the event, and nazmul as a user who performs the event. Process is
//			// not specified in the obligation
//			getPDP(ngacGraph, obligation).getEPP().processEvent(
//					new CreateEvent(ngacGraph.getNode(Constants.PDS_ORIGINATING_OA)), "nazmul", "process");
//
//			// asserting that User nazmul was assigned to PI User Attribute
//			assertTrue(ngacGraph.getChildren("PI").contains("nazmul"));
//			// asserting that User ChairCSUser was assigned to Chair User Attribute
//			assertTrue(ngacGraph.getChildren("Chair").contains("ChairCSUser"));
//			// asserting that User bmCSUser was assigned to Business Manager User Attribute
//			assertTrue(ngacGraph.getChildren("Business Manager").contains("bmCSUser"));
//			// asserting that User DeanCOEUser was assigned to Dean User Attribute
//			assertTrue(ngacGraph.getChildren("Dean").contains("DeanCOEUser"));
//		} catch (PMException e) {
//			e.printStackTrace();
//		}
//
//	}
	@Test
	public void chairApproveProposalTest() {
		try {
			if (ngacGraph.exists("super_pc_rep")) {
				ngacGraph.deleteNode("super_pc_rep");
			}
			PDP pdp = getPDP(ngacGraph, obligation);
			pdp.getEPP().processEvent(new SubmitEvent(ngacGraph.getNode("PDSWhole"), true),
					"nazmul", "process");
			if (ngacGraph.exists("super_pc_rep")) {
				ngacGraph.deleteNode("super_pc_rep");
			}
			
			PReviewDecider decider = new PReviewDecider(ngacGraph);
			assertTrue(decider.check("nazmul", "", "SignatureInfo", "write"));
			
			
			getPDP(ngacGraph, obligation).getEPP().processEvent(new ApproveEvent(ngacGraph.getNode(Constants.CHAIR_APPROVAL)), "ChairCSUser", "process");
			assertTrue(ngacGraph.getChildren("IRBOfficer").contains("irbUser"));
			String PIUser = "nazmul";
			Map<String, OperationSet> map = ngacGraph.getSourceAssociations(PIUser);
			for(Map.Entry<String,OperationSet> entry : map.entrySet()) {
				String tagetNode = entry.getKey();
				OperationSet os = entry.getValue();
				assertTrue(os.contains("write"));
				assertTrue(tagetNode.equals("SignatureInfo"));
			}
			
			assertTrue(decider.check("IRBOfficer", "", "PDSSections", "read"));
			//assertTrue(decider.check("IRBOfficer", "", "IRBApproval", "write"));
			for(String s : ngacGraph.getChildren("IRBOfficer")){
				System.out.println("IRB Children Users" + s);
				for (Map.Entry<String,String> entry : ngacGraph.getNode(s).getProperties().entrySet())  {
					System.out.println(entry.getKey()+" " + entry.getValue());
					assertTrue(entry.getKey().equals("required"));
					assertTrue(entry.getValue().equals("true"));
			}}

		} catch (PMException e) {
			e.printStackTrace();
		}

	}
	// method to get the file object from specified path
	private File getFileFromResources(String fileName) {
		ClassLoader classLoader = this.getClass().getClassLoader();

		URL resource = classLoader.getResource(fileName);
		if (resource == null) {
			throw new IllegalArgumentException("file is not found!");
		} else {
			return new File(resource.getFile());
		}

	}

	// the method to get pdp and load it with custom functions, graph, and obligations. 
	public static PDP getPDP(Graph graph, Obligation obligation) throws PMException {
		// Custom functions
		DeleteNodeExecutor deleteNodeExecutor = new DeleteNodeExecutor();
		CreateNodeExecutor1 createNodeExecutor1 = new CreateNodeExecutor1();
		ConcatExecutor concatExecutor = new ConcatExecutor();
		IsNodeInListExecutor areSomeNodesContainedInExecutor = new IsNodeInListExecutor();
		CompareNodeNamesExecutor compareNodesExecutor = new CompareNodeNamesExecutor();
		CoPIToAddExecutor coPIToAddExecutor = new CoPIToAddExecutor();
		SPToAddExecutor spToAddExecutor = new SPToAddExecutor();
		CoPIToDeleteExecutor coPIToDeleteExecutor = new CoPIToDeleteExecutor();
		SPToDeleteExecutor spToDeleteExecutor = new SPToDeleteExecutor();
		AddPropertiesToNodeExecutor addPropertiesToNodeExecutor = new AddPropertiesToNodeExecutor();
		RemovePropertyFromChildrenExecutor removePropertiesFromChildrenExecutor = new RemovePropertyFromChildrenExecutor();
		AllChildrenHavePropertiesExecutor allChildrenHavePropertiesExecutor = new AllChildrenHavePropertiesExecutor();
		IRBApprovalRequired iRBApprovalRequired = new IRBApprovalRequired();
		GetAncestorInPCExecutor getAncestorInPCExecutor = new GetAncestorInPCExecutor();
		GetChildInPCExecutor getChildInPCExecutor = new GetChildInPCExecutor();
		GetChildrenUsersInPolicyClassExecutor getChildrenInPCExecutor = new GetChildrenUsersInPolicyClassExecutor();
		GetChildExecutor getChildExecutor = new GetChildExecutor();
		GetAncestorsInPCExecutor getAncestorsInPCExecutor = new GetAncestorsInPCExecutor();
		//adding custom functions to eppOptions
		EPPOptions eppOptions = new EPPOptions(deleteNodeExecutor, createNodeExecutor1, concatExecutor,
				areSomeNodesContainedInExecutor, compareNodesExecutor, coPIToAddExecutor, spToAddExecutor,
				coPIToDeleteExecutor, spToDeleteExecutor, addPropertiesToNodeExecutor,
				removePropertiesFromChildrenExecutor, allChildrenHavePropertiesExecutor, iRBApprovalRequired,
				getAncestorInPCExecutor, getChildInPCExecutor, getChildrenInPCExecutor, getChildExecutor,
				getAncestorsInPCExecutor);
		//creating the pdp
		PDP pdp = new PDP(new PAP(graph, new MemProhibitions(), new MemObligations()), eppOptions);
		//adding obligations to the pdp through pap
		pdp.getPAP().getObligationsPAP().add(obligation, true);

		return pdp;
	}
}