package gpms.ngac.policy;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
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
import gov.nist.csd.pm.pip.graph.model.nodes.Node;
import gov.nist.csd.pm.pip.obligations.MemObligations;
import gov.nist.csd.pm.pip.obligations.evr.EVRParser;
import gov.nist.csd.pm.pip.obligations.model.Obligation;
import gov.nist.csd.pm.pip.prohibitions.MemProhibitions;
import gpms.policy.Constants;
import gpms.policy.customEvents.*;
import gpms.policy.customFunctions.*;


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
		// File for administration policy
		File file_administration_policy = getFileFromResources(Constants.POLICY_CONFIG_ADMINISTRATION_UNITS_POLICY_CLASS);
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
		
		String jsonAdministrationPolicy = new String(Files.readAllBytes(Paths.get(file_administration_policy.getAbsolutePath())));

		// Adding super policy to graph
		GraphSerializer.fromJson(ngacGraph, super_policy);
		// Adding eligibility policy to graph
		GraphSerializer.fromJson(ngacGraph, eligibility_policy);
		// Adding organization policy to graph policy to graph
		GraphSerializer.fromJson(ngacGraph, organization_policy);
		GraphSerializer.fromJson(ngacGraph, jsonAdministrationPolicy);
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
//	
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
//			getGPMSpdp(ngacGraph, obligation).getEPP().processEvent(
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
//	}
//	@Test
//	public void chairApproveProposalTest() {
//		try {
//			if (ngacGraph.exists("super_pc_rep")) {
//				ngacGraph.deleteNode("super_pc_rep");
//			}
//			PDP pdp = getPDP(ngacGraph, obligation);
//			pdp.getEPP().processEvent(new SubmitEvent(ngacGraph.getNode("PDSWhole"), true),
//					"nazmul", "process");
//			if (ngacGraph.exists("super_pc_rep")) {
//				ngacGraph.deleteNode("super_pc_rep");
//			}
//			
//			PReviewDecider decider = new PReviewDecider(ngacGraph);
//			assertTrue(decider.check("nazmul", "", "SignatureInfo", "write"));
//			
//			
//			getPDP(ngacGraph, obligation).getEPP().processEvent(new ApproveEvent(ngacGraph.getNode(Constants.CHAIR_APPROVAL)), "ChairCSUser", "process");
//			assertTrue(ngacGraph.getChildren("IRBOfficer").contains("irbUser"));
//			String PIUser = "nazmul";
//			Map<String, OperationSet> map = ngacGraph.getSourceAssociations(PIUser);
//			for(Map.Entry<String,OperationSet> entry : map.entrySet()) {
//				String tagetNode = entry.getKey();
//				OperationSet os = entry.getValue();
//				String[] array = new String[2];
//				array[0] = "write";
//				array[1] = "read";
//				os.containsAll(Arrays.asList(array));
//				assertTrue(os.containsAll(Arrays.asList(array)));
//				assertTrue(os.contains("write"));
//				assertTrue(tagetNode.equals("SignatureInfo"));
//			}
//			
//			assertTrue(decider.check("IRBOfficer", "", "PDSSections", "read"));
//			//assertTrue(decider.check("IRBOfficer", "", "IRBApproval", "write"));
//			for(String s : ngacGraph.getChildren("IRBOfficer")){
//				System.out.println("IRB Children Users" + s);
//				for (Map.Entry<String,String> entry : ngacGraph.getNode(s).getProperties().entrySet())  {
//					System.out.println(entry.getKey()+" " + entry.getValue());
//					assertTrue(entry.getKey().equals("required"));
//					assertTrue(entry.getValue().equals("true"));
//			}}
//
//		} catch (PMException e) {
//			e.printStackTrace();
//		}
//	}

	@Test
	//a whole link from create proposal to chair approval
	public void allApprove() {
		try {
			PReviewDecider decider = new PReviewDecider(ngacGraph);
			PDP pdp = getGPMSpdp(ngacGraph, obligation);
			Node target_copi = ngacGraph.getNode("CoPI");
			Node copiToAdd = ngacGraph.getNode("tomtom");
			Node target_sp = ngacGraph.getNode("SP");
			Node spToAdd = ngacGraph.getNode("vlad");

			//init proposal
			assertFalse(ngacGraph.getChildren("PI").contains("nazmul"));
			assertFalse(decider.check("nazmul", "", "PDSWhole", "Submit"));
			assertFalse(decider.check("nazmul", "", "PDSWhole", "Delete"));
			assertFalse(decider.check("nazmul", "", "PIEditable", "write"));
			assertFalse(decider.check("nazmul", "", "CoPI", "add-copi"));
			assertFalse(decider.check("nazmul", "", "CoPI", "delete-copi"));
			assertFalse(ngacGraph.getChildren("Chair").contains("ChairCSUser"));
			assertFalse(ngacGraph.getChildren("Business Manager").contains("bmCSUser"));
			assertFalse(ngacGraph.getChildren("Dean").contains("DeanCOEUser"));
			pdp.getEPP().processEvent(new CreateEvent(ngacGraph.getNode(Constants.PDS_ORIGINATING_OA)), "nazmul", "process");
			assertTrue(ngacGraph.getChildren("PI").contains("nazmul"));
			assertTrue(decider.check("nazmul", "", "PDSWhole", "Submit"));
			assertTrue(decider.check("nazmul", "", "PDSWhole", "Delete"));
			assertTrue(decider.check("nazmul", "", "PIEditable", "write"));
			assertTrue(decider.check("nazmul", "", "CoPI", "add-copi"));
			assertTrue(decider.check("nazmul", "", "CoPI", "delete-copi"));
			assertTrue(ngacGraph.getChildren("Chair").contains("ChairCSUser"));
			assertTrue(ngacGraph.getChildren("Business Manager").contains("bmCSUser"));
			assertTrue(ngacGraph.getChildren("Dean").contains("DeanCOEUser"));

			ngacGraph = getGPMSpdp(ngacGraph, obligation).getPAP().getGraphPAP();

			//add Copi
			assertFalse(decider.check("tomtom", "", "CoPIInfo", "read"));
			assertFalse(ngacGraph.getChildren("CoPI").contains("tomtom"));
			assertFalse(ngacGraph.getChildren("Chair").contains("ChairChemUser"));
			assertFalse(ngacGraph.getChildren("Business Manager").contains("bmChemUser"));
			assertFalse(ngacGraph.getChildren("Dean").contains("DeanCOASUser"));
			assertFalse(decider.check("tomtom", "", "CoPIEditable", "write"));
			pdp.getEPP().processEvent(new AddCoPIEvent(target_copi, copiToAdd), "nazmul", "process");
			assertTrue(decider.check("tomtom", "", "CoPIInfo", "read"));
			assertTrue(ngacGraph.getChildren("CoPI").contains("tomtom"));
			assertTrue(ngacGraph.getChildren("Chair").contains("ChairChemUser"));//asser error
			assertTrue(ngacGraph.getChildren("Business Manager").contains("bmChemUser"));
			assertTrue(ngacGraph.getChildren("Dean").contains("DeanCOASUser"));
			assertTrue(decider.check("tomtom", "", "CoPIEditable", "read"));

			//add SP
			assertFalse(ngacGraph.getChildren("SP").contains("vlad"));
			pdp.getEPP().processEvent(new AddSPEvent(target_sp, spToAdd), "tomtom", "process");
			assertTrue(ngacGraph.getChildren("SP").contains("vlad"));

			//submit proposal
			assertFalse(decider.check("ChairCSUser", "", "PDSSections", "read"));
			assertFalse(decider.check("ChairCSUser", "", "PDSWhole", "approve"));
			assertFalse(decider.check("ChairCSUser", "", "PDSWhole", "disapprove"));
			assertFalse(decider.check("ChairCSUser", "", "ChairApproval", "write"));
			assertFalse(decider.check("ChairChemUser", "", "PDSSections", "read"));
			assertFalse(decider.check("ChairChemUser", "", "PDSWhole", "approve"));
			assertFalse(decider.check("ChairChemUser", "", "PDSWhole", "disapprove"));
			assertFalse(decider.check("ChairChemUser", "", "ChairApproval", "write"));
			pdp.getEPP().processEvent(new SubmitEvent(ngacGraph.getNode("PDSWhole"), true),"nazmul", "process");
			assertFalse(decider.check("nazmul", "", "PDSWhole", "Submit"));
			assertFalse(decider.check("nazmul", "", "PDSWhole", "Delete"));
			assertFalse(decider.check("nazmul", "", "PIEditable", "write"));
			assertFalse(decider.check("nazmul", "", "CoPI", "add-copi"));
			assertFalse(decider.check("nazmul", "", "CoPI", "delete-copi"));
			assertFalse(decider.check("tomtom", "", "CoPIEditable", "write"));
			assertFalse(decider.check("tomtom", "", "CoPI", "add-sp"));
			assertFalse(decider.check("tomtom", "", "CoPI", "delete-sp"));
			assertTrue(decider.check("ChairCSUser", "", "PDSSections", "read"));
			assertTrue(decider.check("ChairCSUser", "", "PDSWhole", "Approve"));
			assertTrue(decider.check("ChairCSUser", "", "PDSWhole", "Disapprove"));
			assertTrue(decider.check("ChairCSUser", "", "ChairApproval", "write"));
			assertTrue(decider.check("ChairChemUser", "", "PDSSections", "read"));
			assertTrue(decider.check("ChairChemUser", "", "PDSWhole", "Approve"));
			assertTrue(decider.check("ChairChemUser", "", "PDSWhole", "Disapprove"));
			assertTrue(decider.check("ChairChemUser", "", "ChairApproval", "write"));
			
			//chair approve
			pdp.getEPP().processEvent(new ApproveEvent(ngacGraph.getNode(Constants.CHAIR_APPROVAL)), "ChairCSUser", "process");
			pdp.getEPP().processEvent(new ApproveEvent(ngacGraph.getNode(Constants.CHAIR_APPROVAL)), "ChairChemUser", "process");
			assertFalse(decider.check("ChairCSUser", "", "PDSWhole", "Approve"));
			assertFalse(decider.check("ChairCSUser", "", "PDSWhole", "Disapprove"));
			assertFalse(decider.check("ChairCSUser", "", "ChairApproval", "write"));
			assertFalse(decider.check("ChairChemUser", "", "PDSWhole", "Approve"));
			assertFalse(decider.check("ChairChemUser", "", "PDSWhole", "Disapprove"));
			assertFalse(decider.check("ChairChemUser", "", "ChairApproval", "write"));	
			
			assertTrue(decider.check("bmCSUser", "", "PDSSections", "read"));
			assertTrue(decider.check("bmCSUser", "", "PDSWhole", "Approve"));
			assertTrue(decider.check("bmCSUser", "", "PDSWhole", "Disapprove"));
			assertTrue(decider.check("bmCSUser", "", "BMApproval", "write"));
			assertTrue(decider.check("bmChemUser", "", "PDSSections", "read"));
			assertTrue(decider.check("bmChemUser", "", "PDSWhole", "Approve"));
			assertTrue(decider.check("bmChemUser", "", "PDSWhole", "Disapprove"));
			assertTrue(decider.check("bmChemUser", "", "BMApproval", "write"));
			
			assertTrue(decider.check("irbUser", "", "PDSSections", "read"));
			assertTrue(decider.check("irbUser", "", "PDSWhole", "Approve"));
			assertTrue(decider.check("irbUser", "", "PDSWhole", "Disapprove"));
			assertTrue(decider.check("irbUser", "", "IRBApproval", "write"));

			//BM approve
			pdp.getEPP().processEvent(new ApproveEvent(ngacGraph.getNode(Constants.BM_APPROVAL)), "bmCSUser", "process");
			pdp.getEPP().processEvent(new ApproveEvent(ngacGraph.getNode(Constants.BM_APPROVAL)), "bmChemUser", "process");
			assertFalse(decider.check("bmCSUser", "", "PDSWhole", "Approve"));
			assertFalse(decider.check("bmCSUser", "", "PDSWhole", "Disapprove"));
			assertFalse(decider.check("bmCSUser", "", "BMApproval", "write"));
			assertFalse(decider.check("bmChemUser", "", "PDSWhole", "Approve"));
			assertFalse(decider.check("bmChemUser", "", "PDSWhole", "Disapprove"));
			assertFalse(decider.check("bmChemUser", "", "BMApproval", "write"));
			
			assertTrue(decider.check("DeanCOEUser", "", "PDSSections", "read"));
			assertTrue(decider.check("DeanCOEUser", "", "PDSWhole", "Approve"));
			assertTrue(decider.check("DeanCOEUser", "", "PDSWhole", "Disapprove"));
			assertTrue(decider.check("DeanCOEUser", "", "DeanApproval", "write"));
			assertTrue(decider.check("DeanCOASUser", "", "PDSSections", "read"));
			assertTrue(decider.check("DeanCOASUser", "", "PDSWhole", "Approve"));
			assertTrue(decider.check("DeanCOASUser", "", "PDSWhole", "Disapprove"));
			assertTrue(decider.check("DeanCOASUser", "", "DeanApproval", "write"));
			
			//IRB approve
			pdp.getEPP().processEvent(new ApproveEvent(ngacGraph.getNode(Constants.IRB_APPROVAL)),"irbUser", "process");
			assertFalse(decider.check("irbUser", "", "PDSWhole", "Approve"));
			assertFalse(decider.check("irbUser", "", "PDSWhole", "Disapprove"));
			assertFalse(decider.check("irbUser", "", "IRBApproval", "write"));
			
			//Dean approve
			pdp.getEPP().processEvent(new ApproveEvent(ngacGraph.getNode(Constants.DEAN_APPROVAL)), "DeanCOEUser", "process");
			pdp.getEPP().processEvent(new ApproveEvent(ngacGraph.getNode(Constants.DEAN_APPROVAL)), "DeanCOASUser", "process");
			assertTrue(decider.check("raUser", "", "PDSSections", "read"));
			assertTrue(decider.check("raUser", "", "PDSWhole", "Approve"));
			assertTrue(decider.check("raUser", "", "PDSWhole", "Disapprove"));
			assertTrue(decider.check("raUser", "", "PDSWhole", "Withdraw"));
			assertTrue(decider.check("raUser", "", "RAApproval", "write"));
			assertTrue(decider.check("raUser", "", "OSPInfo", "write"));
			
			//RA approve
			pdp.getEPP().processEvent(new ApproveEvent(ngacGraph.getNode(Constants.RA_APPROVAL)), "raUser", "process");
			assertFalse(decider.check("raUser", "", "PDSWhole", "Approve"));
			assertFalse(decider.check("raUser", "", "PDSWhole", "Disapprove"));
			assertFalse(decider.check("raUser", "", "PDSWhole", "Withdraw"));
			assertFalse(decider.check("raUser", "", "RAApproval", "write"));
			assertFalse(decider.check("raUser", "", "OSPInfo", "write"));
			
			assertTrue(decider.check("rdUser", "", "PDSWhole", "Approve"));
			assertTrue(decider.check("rdUser", "", "PDSWhole", "Disapprove"));
			assertTrue(decider.check("rdUser", "", "PDSWhole", "Delete"));
			assertTrue(decider.check("rdUser", "", "RDApproval", "write"));
			assertTrue(decider.check("rdUser", "", "OSPInfo", "write"));

			//RD approve
			pdp.getEPP().processEvent(new ApproveEvent(ngacGraph.getNode(Constants.RD_APPROVAL)), "rdUser", "process");
			assertTrue(decider.check("raUser", "", "PDSWhole", "Submit"));
			assertTrue(decider.check("raUser", "", "RAApproval", "write"));
			
			assertFalse(decider.check("rdUser", "", "PDSWhole", "Approve"));
			assertFalse(decider.check("rdUser", "", "PDSWhole", "Disapprove"));
			assertFalse(decider.check("rdUser", "", "PDSWhole", "Delete"));
			assertFalse(decider.check("rdUser", "", "RDApproval", "write"));
			assertFalse(decider.check("rdUser", "", "OSPInfo", "write"));

			//RA submit
			pdp.getEPP().processEvent(new SubmitRAEvent(ngacGraph.getNode("PDSWhole")), "raUser", "process");
			assertFalse(decider.check("raUser", "", "PDSWhole", "Submit"));
			assertFalse(decider.check("raUser", "", "RAApproval", "write"));
			
			assertTrue(decider.check("rdUser", "", "PDSWhole", "Archive"));
			assertTrue(decider.check("rdUser", "", "RDApproval", "write"));
			
		} catch (PMException e) {
			e.printStackTrace();
		}
	}
	
	@Test
	//include delete_copi/delete_sp
	public void deleteCoPISP() {
		try {
			PReviewDecider decider = new PReviewDecider(ngacGraph);
			Node target_copi = ngacGraph.getNode("CoPI");
			Node copiToAdd = ngacGraph.getNode("tomtom");
			Node target_sp = ngacGraph.getNode("SP");
			Node spToAdd = ngacGraph.getNode("vlad");
			
			//init proposal
			PDP pdp = getGPMSpdp(ngacGraph, obligation);
			pdp.getEPP().processEvent(new CreateEvent(ngacGraph.getNode(Constants.PDS_ORIGINATING_OA)), "nazmul", "process");
     		ngacGraph = getGPMSpdp(ngacGraph, obligation).getPAP().getGraphPAP();
     		
     		//add Copi				
			assertFalse(decider.check("tomtom", "", "CoPIInfo", "read"));
			assertFalse(ngacGraph.getChildren("CoPI").contains("tomtom"));
			assertFalse(ngacGraph.getChildren("Chair").contains("ChairChemUser"));
			assertFalse(ngacGraph.getChildren("Business Manager").contains("bmChemUser"));
			assertFalse(ngacGraph.getChildren("Dean").contains("DeanCOASUser"));
			assertFalse(decider.check("tomtom", "", "CoPIEditable", "write"));
			pdp.getEPP().processEvent(new AddCoPIEvent(target_copi, copiToAdd), "nazmul", "process");	
			assertTrue(decider.check("tomtom", "", "CoPIInfo", "read"));
			assertTrue(ngacGraph.getChildren("CoPI").contains("tomtom"));
			assertTrue(ngacGraph.getChildren("Chair").contains("ChairChemUser"));//asser error
			assertTrue(ngacGraph.getChildren("Business Manager").contains("bmChemUser"));
			assertTrue(ngacGraph.getChildren("Dean").contains("DeanCOASUser"));
			assertTrue(decider.check("tomtom", "", "CoPIEditable", "read"));
			
			//add SP
			assertFalse(ngacGraph.getChildren("SP").contains("vlad"));
			pdp.getEPP().processEvent(new AddSPEvent(target_sp, spToAdd), "tomtom", "process");
			assertTrue(ngacGraph.getChildren("SP").contains("vlad"));
			
			//delete SP
			pdp.getEPP().processEvent(new DeleteSPEvent(target_sp, spToAdd), "tomtom", "process");
			assertFalse(ngacGraph.getChildren("SP").contains("vlad"));
			
			//delete CoPI
			pdp.getEPP().processEvent(new DeleteCoPIEvent(target_copi, copiToAdd), "nazmul", "process");
			assertFalse(decider.check("tomtom", "", "CoPIInfo", "read"));
			assertFalse(ngacGraph.getChildren("CoPI").contains("tomtom"));
			assertFalse(ngacGraph.getChildren("Chair").contains("ChairChemUser"));
			assertFalse(ngacGraph.getChildren("Business Manager").contains("bmChemUser"));
			assertFalse(ngacGraph.getChildren("Dean").contains("DeanCOASUser"));
			assertFalse(decider.check("tomtom", "", "CoPIEditable", "write"));
     	} catch (PMException e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void chairDisapprove() {
		try {
			PReviewDecider decider = new PReviewDecider(ngacGraph);
			
			//init proposal
			PDP pdp = getGPMSpdp(ngacGraph, obligation);
			pdp.getEPP().processEvent(new CreateEvent(ngacGraph.getNode(Constants.PDS_ORIGINATING_OA)), "nazmul", "process");
     		ngacGraph = getGPMSpdp(ngacGraph, obligation).getPAP().getGraphPAP();
     		
     		//add Copi and SP
			pdp.getEPP().processEvent(new AddCoPIEvent(ngacGraph.getNode("CoPI"), ngacGraph.getNode("tomtom")), "nazmul", "process");	
			pdp.getEPP().processEvent(new AddSPEvent(ngacGraph.getNode("SP"), ngacGraph.getNode("vlad")), "tomtom", "process");
			
			//submit proposal
			pdp.getEPP().processEvent(new SubmitEvent(ngacGraph.getNode("PDSWhole"), true),"nazmul", "process");
			
			//chair disapproval
//			pdp.getEPP().processEvent(new DisapproveEvent(ngacGraph.getNode(Constants.CHAIR_APPROVAL)), "ChairCSUser", "process");
			pdp.getEPP().processEvent(new DisapproveEvent(ngacGraph.getNode(Constants.CHAIR_APPROVAL)), "ChairChemUser", "process");
			assertFalse(decider.check("ChairCSUser", "", "PDSWhole", "Approve"));
			assertFalse(decider.check("ChairCSUser", "", "PDSWhole", "Disapprove"));
			assertFalse(decider.check("ChairCSUser", "", "ChairApproval", "write"));
			assertFalse(decider.check("ChairCSUser", "", "ChairApproval", "write"));
			assertFalse(decider.check("ChairChemUser", "", "PDSWhole", "Approve"));
			assertFalse(decider.check("ChairChemUser", "", "PDSWhole", "Disapprove"));
			assertFalse(decider.check("ChairChemUser", "", "ChairApproval", "write"));
			assertFalse(decider.check("ChairChemUser", "", "ChairApproval", "write"));
			
			assertTrue(decider.check("nazmul", "", "PDSWhole", "Submit"));
			assertTrue(decider.check("nazmul", "", "PDSWhole", "Delete"));	
			assertTrue(decider.check("nazmul", "", "PIEditable", "write"));
			assertTrue(decider.check("nazmul", "", "CoPI", "add-copi"));
			assertTrue(decider.check("nazmul", "", "CoPI", "delete-copi"));
			assertTrue(decider.check("tomtom", "", "CoPIEditable", "write"));
			assertTrue(decider.check("tomtom", "", "CoPI", "add-sp"));
			assertTrue(decider.check("tomtom", "", "CoPI", "delete-sp"));
     	} catch (PMException e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void bmDisapprove() {
		try {
			PReviewDecider decider = new PReviewDecider(ngacGraph);
		
			//init proposal
			PDP pdp = getGPMSpdp(ngacGraph, obligation);
			pdp.getEPP().processEvent(new CreateEvent(ngacGraph.getNode(Constants.PDS_ORIGINATING_OA)), "nazmul", "process");
			ngacGraph = getGPMSpdp(ngacGraph, obligation).getPAP().getGraphPAP();
 		
			//add Copi and SP
			pdp.getEPP().processEvent(new AddCoPIEvent(ngacGraph.getNode("CoPI"), ngacGraph.getNode("tomtom")), "nazmul", "process");	
			pdp.getEPP().processEvent(new AddSPEvent(ngacGraph.getNode("SP"), ngacGraph.getNode("vlad")), "tomtom", "process");
		
			//submit proposal
			pdp.getEPP().processEvent(new SubmitEvent(ngacGraph.getNode("PDSWhole"), true),"nazmul", "process");
		
			//Chairs approve
			pdp.getEPP().processEvent(new ApproveEvent(ngacGraph.getNode(Constants.CHAIR_APPROVAL)), "ChairCSUser", "process");
			pdp.getEPP().processEvent(new ApproveEvent(ngacGraph.getNode(Constants.CHAIR_APPROVAL)), "ChairChemUser", "process");
			
			//step4: BMs disapprove
			pdp.getEPP().processEvent(new DisapproveEvent(ngacGraph.getNode(Constants.BM_APPROVAL)), "bmCSUser", "process");
//			pdp.getEPP().processEvent(new DisapproveEvent(ngacGraph.getNode(Constants.BM_APPROVAL)), "bmChemUser", "process");
			assertFalse(decider.check("bmCSUser", "", "PDSSections", "read"));
			assertFalse(decider.check("bmCSUser", "", "PDSWhole", "Approve"));
			assertFalse(decider.check("bmCSUser", "", "PDSWhole", "Disapprove"));
			assertFalse(decider.check("bmCSUser", "", "BMApproval", "write"));
			assertFalse(decider.check("bmChemUser", "", "PDSSections", "read"));	
			assertFalse(decider.check("bmChemUser", "", "PDSWhole", "Approve"));
			assertFalse(decider.check("bmChemUser", "", "PDSWhole", "Disapprove"));
			assertFalse(decider.check("bmChemUser", "", "BMApproval", "write"));
			
			assertFalse(decider.check("irbUser", "", "PDSWhole", "Approve"));
			assertFalse(decider.check("irbUser", "", "PDSWhole", "Disapprove"));
			assertFalse(decider.check("irbUser", "", "PDSSections", "read"));
			assertFalse(decider.check("irbUser", "", "IRBApproval", "write"));
			
			assertTrue(decider.check("nazmul", "", "PDSWhole", "Submit"));
			assertTrue(decider.check("nazmul", "", "PDSWhole", "Delete"));	
			assertTrue(decider.check("nazmul", "", "PIEditable", "write"));
			assertTrue(decider.check("nazmul", "", "CoPI", "add-copi"));
			assertTrue(decider.check("nazmul", "", "CoPI", "delete-copi"));
			assertTrue(decider.check("tomtom", "", "CoPIEditable", "write"));
			assertTrue(decider.check("tomtom", "", "CoPI", "add-sp"));
			assertTrue(decider.check("tomtom", "", "CoPI", "delete-sp"));
			
			assertFalse(decider.check("ChairCSUser", "", "PDSSections", "read"));
			assertFalse(decider.check("ChairChemUser", "", "PDSSections", "read"));
     	} catch (PMException e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void deanDisapprove() {
		try {
			PReviewDecider decider = new PReviewDecider(ngacGraph);

			//init proposal
			PDP pdp = getGPMSpdp(ngacGraph, obligation);
			pdp.getEPP().processEvent(new CreateEvent(ngacGraph.getNode(Constants.PDS_ORIGINATING_OA)), "nazmul", "process");
			ngacGraph = getGPMSpdp(ngacGraph, obligation).getPAP().getGraphPAP();
 		
			//add Copi and SP
			pdp.getEPP().processEvent(new AddCoPIEvent(ngacGraph.getNode("CoPI"), ngacGraph.getNode("tomtom")), "nazmul", "process");	
			pdp.getEPP().processEvent(new AddSPEvent(ngacGraph.getNode("SP"), ngacGraph.getNode("vlad")), "tomtom", "process");
		
			//submit proposal
			pdp.getEPP().processEvent(new SubmitEvent(ngacGraph.getNode("PDSWhole"), true),"nazmul", "process");
		
			//Chairs approve
			pdp.getEPP().processEvent(new ApproveEvent(ngacGraph.getNode(Constants.CHAIR_APPROVAL)), "ChairCSUser", "process");
			pdp.getEPP().processEvent(new ApproveEvent(ngacGraph.getNode(Constants.CHAIR_APPROVAL)), "ChairChemUser", "process");

			//BMs approve
			pdp.getEPP().processEvent(new ApproveEvent(ngacGraph.getNode(Constants.BM_APPROVAL)), "bmCSUser", "process");
			pdp.getEPP().processEvent(new ApproveEvent(ngacGraph.getNode(Constants.BM_APPROVAL)), "bmChemUser", "process");
			
			//step8: Deans disapprove
			pdp.getEPP().processEvent(new DisapproveEvent(ngacGraph.getNode(Constants.DEAN_APPROVAL)), "DeanCOEUser", "process");
//			pdp.getEPP().processEvent(new DisapproveEvent(ngacGraph.getNode(Constants.DEAN_APPROVAL)), "DeanCOAUser", "process");
			assertFalse(decider.check("DeanCOEUser", "", "PDSSections", "read"));
			assertFalse(decider.check("DeanCOEUser", "", "PDSWhole", "Approve"));
			assertFalse(decider.check("DeanCOEUser", "", "PDSWhole", "Disapprove"));
			assertFalse(decider.check("DeanCOEUser", "", "DeanApproval", "write"));
			assertFalse(decider.check("DeanCOASUser", "", "PDSSections", "read"));
			assertFalse(decider.check("DeanCOASUser", "", "PDSWhole", "Approve"));
			assertFalse(decider.check("DeanCOASUser", "", "PDSWhole", "Disapprove"));
			assertFalse(decider.check("DeanCOASUser", "", "DeanApproval", "write"));
			
			assertFalse(decider.check("irbUser", "", "PDSWhole", "Approve"));
			assertFalse(decider.check("irbUser", "", "PDSWhole", "Disapprove"));
			assertFalse(decider.check("irbUser", "", "IRBApproval", "write"));
			assertFalse(decider.check("irbUser", "", "PDSSections", "read"));
			
			assertTrue(decider.check("nazmul", "", "PDSWhole", "Submit"));
			assertTrue(decider.check("nazmul", "", "PDSWhole", "Delete"));	
			assertTrue(decider.check("nazmul", "", "PIEditable", "write"));
			assertTrue(decider.check("nazmul", "", "CoPI", "add-copi"));
			assertTrue(decider.check("nazmul", "", "CoPI", "delete-copi"));
			assertTrue(decider.check("tomtom", "", "CoPIEditable", "write"));
			assertTrue(decider.check("tomtom", "", "CoPI", "add-sp"));
			assertTrue(decider.check("tomtom", "", "CoPI", "delete-sp"));
			
			assertFalse(decider.check("ChairCSUser", "", "PDSSections", "read"));
			assertFalse(decider.check("ChairChemUser", "", "PDSSections", "read"));
			
			assertFalse(decider.check("bmCSUser", "", "PDSSections", "read"));
			assertFalse(decider.check("bmChemUser", "", "PDSSections", "read"));
     	} catch (PMException e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void irbDisapprove() {
		try {
			PReviewDecider decider = new PReviewDecider(ngacGraph);

			//init proposal
			PDP pdp = getGPMSpdp(ngacGraph, obligation);
			pdp.getEPP().processEvent(new CreateEvent(ngacGraph.getNode(Constants.PDS_ORIGINATING_OA)), "nazmul", "process");
			ngacGraph = getGPMSpdp(ngacGraph, obligation).getPAP().getGraphPAP();
 		
			//add Copi and SP
			pdp.getEPP().processEvent(new AddCoPIEvent(ngacGraph.getNode("CoPI"), ngacGraph.getNode("tomtom")), "nazmul", "process");	
			pdp.getEPP().processEvent(new AddSPEvent(ngacGraph.getNode("SP"), ngacGraph.getNode("vlad")), "tomtom", "process");
		
			//submit proposal
			pdp.getEPP().processEvent(new SubmitEvent(ngacGraph.getNode("PDSWhole"), true),"nazmul", "process");
		
			//Chairs approve
			pdp.getEPP().processEvent(new ApproveEvent(ngacGraph.getNode(Constants.CHAIR_APPROVAL)), "ChairCSUser", "process");
			pdp.getEPP().processEvent(new ApproveEvent(ngacGraph.getNode(Constants.CHAIR_APPROVAL)), "ChairChemUser", "process");

			//BMs approve
			pdp.getEPP().processEvent(new ApproveEvent(ngacGraph.getNode(Constants.BM_APPROVAL)), "bmCSUser", "process");
			pdp.getEPP().processEvent(new ApproveEvent(ngacGraph.getNode(Constants.BM_APPROVAL)), "bmChemUser", "process");
			
			//Deans approve
			pdp.getEPP().processEvent(new ApproveEvent(ngacGraph.getNode(Constants.DEAN_APPROVAL)), "DeanCOEUser", "process");
			pdp.getEPP().processEvent(new ApproveEvent(ngacGraph.getNode(Constants.DEAN_APPROVAL)), "DeanCOASUser", "process");
			
			//IRB disapprove
			pdp.getEPP().processEvent(new DisapproveEvent(ngacGraph.getNode(Constants.IRB_APPROVAL)), "irbUser", "process");
			assertFalse(decider.check("DeanCOEUser", "", "PDSSections", "read"));
			assertFalse(decider.check("DeanCOEUser", "", "PDSWhole", "Approve"));
			assertFalse(decider.check("DeanCOEUser", "", "PDSWhole", "Disapprove"));
			assertFalse(decider.check("DeanCOEUser", "", "DeanApproval", "write"));
			assertFalse(decider.check("DeanCOASUser", "", "PDSSections", "read"));
			assertFalse(decider.check("DeanCOASUser", "", "PDSWhole", "Approve"));
			assertFalse(decider.check("DeanCOASUser", "", "PDSWhole", "Disapprove"));
			assertFalse(decider.check("DeanCOASUser", "", "DeanApproval", "write"));
			
			assertFalse(decider.check("irbUser", "", "PDSWhole", "Approve"));
			assertFalse(decider.check("irbUser", "", "PDSWhole", "Disapprove"));
			assertFalse(decider.check("irbUser", "", "IRBApproval", "write"));
			assertFalse(decider.check("irbUser", "", "PDSSections", "read"));
			
			assertTrue(decider.check("nazmul", "", "PDSWhole", "Submit"));
			assertTrue(decider.check("nazmul", "", "PDSWhole", "Delete"));	
			assertTrue(decider.check("nazmul", "", "PIEditable", "write"));
			assertTrue(decider.check("nazmul", "", "CoPI", "add-copi"));
			assertTrue(decider.check("nazmul", "", "CoPI", "delete-copi"));
			assertTrue(decider.check("tomtom", "", "CoPIEditable", "write"));
			assertTrue(decider.check("tomtom", "", "CoPI", "add-sp"));
			assertTrue(decider.check("tomtom", "", "CoPI", "delete-sp"));
			
			assertFalse(decider.check("ChairCSUser", "", "PDSSections", "read"));
			assertFalse(decider.check("ChairChemUser", "", "PDSSections", "read"));
			
			assertFalse(decider.check("bmCSUser", "", "PDSSections", "read"));
			assertFalse(decider.check("bmChemUser", "", "PDSSections", "read"));
     	} catch (PMException e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void raDisapprove() {
		try {
			PReviewDecider decider = new PReviewDecider(ngacGraph);

			//init proposal
			PDP pdp = getGPMSpdp(ngacGraph, obligation);
			pdp.getEPP().processEvent(new CreateEvent(ngacGraph.getNode(Constants.PDS_ORIGINATING_OA)), "nazmul", "process");
			ngacGraph = getGPMSpdp(ngacGraph, obligation).getPAP().getGraphPAP();
 		
			//add Copi and SP
			pdp.getEPP().processEvent(new AddCoPIEvent(ngacGraph.getNode("CoPI"), ngacGraph.getNode("tomtom")), "nazmul", "process");	
			pdp.getEPP().processEvent(new AddSPEvent(ngacGraph.getNode("SP"), ngacGraph.getNode("vlad")), "tomtom", "process");
		
			//submit proposal
			pdp.getEPP().processEvent(new SubmitEvent(ngacGraph.getNode("PDSWhole"), true),"nazmul", "process");
		
			//Chairs approve
			pdp.getEPP().processEvent(new ApproveEvent(ngacGraph.getNode(Constants.CHAIR_APPROVAL)), "ChairCSUser", "process");
			pdp.getEPP().processEvent(new ApproveEvent(ngacGraph.getNode(Constants.CHAIR_APPROVAL)), "ChairChemUser", "process");

			//BMs approve
			pdp.getEPP().processEvent(new ApproveEvent(ngacGraph.getNode(Constants.BM_APPROVAL)), "bmCSUser", "process");
			pdp.getEPP().processEvent(new ApproveEvent(ngacGraph.getNode(Constants.BM_APPROVAL)), "bmChemUser", "process");
			
			//Deans approve
			pdp.getEPP().processEvent(new ApproveEvent(ngacGraph.getNode(Constants.DEAN_APPROVAL)), "DeanCOEUser", "process");
			pdp.getEPP().processEvent(new ApproveEvent(ngacGraph.getNode(Constants.DEAN_APPROVAL)), "DeanCOASUser", "process");
			
			//IRB approve
			pdp.getEPP().processEvent(new ApproveEvent(ngacGraph.getNode(Constants.IRB_APPROVAL)),"irbUser", "process");
			
			//RA disapprove
			pdp.getEPP().processEvent(new DisapproveEvent(ngacGraph.getNode(Constants.RA_APPROVAL)), "raUser", "process");			
			assertFalse(decider.check("raUser", "", "RAApproval", "write"));
			assertFalse(decider.check("raUser", "", "PDSWhole", "Approve"));
			assertFalse(decider.check("raUser", "", "PDSWhole", "Disapprove"));
			assertFalse(decider.check("raUser", "", "PDSWhole", "Withdraw"));
			assertFalse(decider.check("raUser", "", "OSPInfo", "write"));
			assertFalse(decider.check("raUser", "", "PDSSections", "read"));
			
			assertFalse(decider.check("ChairCSUser", "", "PDSSections", "read"));
			assertFalse(decider.check("ChairChemUser", "", "PDSSections", "read"));
			
			assertFalse(decider.check("bmCSUser", "", "PDSSections", "read"));
			assertFalse(decider.check("bmChemUser", "", "PDSSections", "read"));
			
			assertFalse(decider.check("DeanCOEUser", "", "PDSSections", "read"));
			assertFalse(decider.check("DeanCOASUser", "", "PDSSections", "read"));
			
			assertTrue(decider.check("nazmul", "", "PDSWhole", "Submit"));
			assertTrue(decider.check("nazmul", "", "PDSWhole", "Delete"));	
			assertTrue(decider.check("nazmul", "", "PIEditable", "write"));
			assertTrue(decider.check("nazmul", "", "CoPI", "add-copi"));
			assertTrue(decider.check("nazmul", "", "CoPI", "delete-copi"));
			assertTrue(decider.check("tomtom", "", "CoPIEditable", "write"));
			assertTrue(decider.check("tomtom", "", "CoPI", "add-sp"));
			assertTrue(decider.check("tomtom", "", "CoPI", "delete-sp"));
     	} catch (PMException e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void rdDisapprove() {
		try {
			PReviewDecider decider = new PReviewDecider(ngacGraph);

			//init proposal
			PDP pdp = getGPMSpdp(ngacGraph, obligation);
			pdp.getEPP().processEvent(new CreateEvent(ngacGraph.getNode(Constants.PDS_ORIGINATING_OA)), "nazmul", "process");
			ngacGraph = getGPMSpdp(ngacGraph, obligation).getPAP().getGraphPAP();
 		
			//add Copi and SP
			pdp.getEPP().processEvent(new AddCoPIEvent(ngacGraph.getNode("CoPI"), ngacGraph.getNode("tomtom")), "nazmul", "process");	
			pdp.getEPP().processEvent(new AddSPEvent(ngacGraph.getNode("SP"), ngacGraph.getNode("vlad")), "tomtom", "process");
		
			//submit proposal
			pdp.getEPP().processEvent(new SubmitEvent(ngacGraph.getNode("PDSWhole"), true),"nazmul", "process");
		
			//Chairs approve
			pdp.getEPP().processEvent(new ApproveEvent(ngacGraph.getNode(Constants.CHAIR_APPROVAL)), "ChairCSUser", "process");
			pdp.getEPP().processEvent(new ApproveEvent(ngacGraph.getNode(Constants.CHAIR_APPROVAL)), "ChairChemUser", "process");

			//BMs approve
			pdp.getEPP().processEvent(new ApproveEvent(ngacGraph.getNode(Constants.BM_APPROVAL)), "bmCSUser", "process");
			pdp.getEPP().processEvent(new ApproveEvent(ngacGraph.getNode(Constants.BM_APPROVAL)), "bmChemUser", "process");
			
			//Deans approve
			pdp.getEPP().processEvent(new ApproveEvent(ngacGraph.getNode(Constants.DEAN_APPROVAL)), "DeanCOEUser", "process");
			pdp.getEPP().processEvent(new ApproveEvent(ngacGraph.getNode(Constants.DEAN_APPROVAL)), "DeanCOASUser", "process");
			
			//IRB approve
			pdp.getEPP().processEvent(new ApproveEvent(ngacGraph.getNode(Constants.IRB_APPROVAL)),"irbUser", "process");
			
			//RA approve
			pdp.getEPP().processEvent(new ApproveEvent(ngacGraph.getNode(Constants.RA_APPROVAL)), "raUser", "process");
			
			//RD disapprove
			pdp.getEPP().processEvent(new DisapproveEvent(ngacGraph.getNode(Constants.RD_APPROVAL)), "rdUser", "process");
			assertFalse(decider.check("rdUser", "", "RDApproval", "write"));
			assertFalse(decider.check("rdUser", "", "PDSWhole", "Approve"));
			assertFalse(decider.check("rdUser", "", "PDSWhole", "Disapprove"));
			assertFalse(decider.check("rdUser", "", "PDSWhole", "Delete"));	
			assertFalse(decider.check("rdUser", "", "OSPInfo", "write"));
			assertFalse(decider.check("rdUser", "", "PDSSections", "read"));
			
			assertFalse(decider.check("ChairCSUser", "", "PDSSections", "read"));
			assertFalse(decider.check("ChairChemUser", "", "PDSSections", "read"));
			
			assertFalse(decider.check("bmCSUser", "", "PDSSections", "read"));
			assertFalse(decider.check("bmChemUser", "", "PDSSections", "read"));
			
			assertFalse(decider.check("DeanCOEUser", "", "PDSSections", "read"));
			assertFalse(decider.check("DeanCOASUser", "", "PDSSections", "read"));
			
			assertFalse(decider.check("raUser", "", "PDSSections", "read"));
			
			assertTrue(decider.check("nazmul", "", "PDSWhole", "Submit"));
			assertTrue(decider.check("nazmul", "", "PDSWhole", "Delete"));	
			assertTrue(decider.check("nazmul", "", "PIEditable", "write"));
			assertTrue(decider.check("nazmul", "", "CoPI", "add-copi"));
			assertTrue(decider.check("nazmul", "", "CoPI", "delete-copi"));
			assertTrue(decider.check("tomtom", "", "CoPIEditable", "write"));
			assertTrue(decider.check("tomtom", "", "CoPI", "add-sp"));
			assertTrue(decider.check("tomtom", "", "CoPI", "delete-sp"));
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
	public static PDP getGPMSpdp(Graph graph, Obligation obligation) throws PMException {
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
		GetDescendantInPCExecutor getAncestorInPCExecutor = new GetDescendantInPCExecutor();
		GetChildInPCExecutor getChildInPCExecutor = new GetChildInPCExecutor();
		GetChildrenUsersInPolicyClassExecutor getChildrenInPCExecutor = new GetChildrenUsersInPolicyClassExecutor();
		GetChildExecutor getChildExecutor = new GetChildExecutor();
		GetDescendantsInPCExecutor getAncestorsInPCExecutor = new GetDescendantsInPCExecutor();
		//adding custom functions to eppOptions
		EPPOptions eppOptions = new EPPOptions(deleteNodeExecutor, createNodeExecutor1, concatExecutor,
				areSomeNodesContainedInExecutor, compareNodesExecutor, coPIToAddExecutor, spToAddExecutor,
				coPIToDeleteExecutor, spToDeleteExecutor, addPropertiesToNodeExecutor,
				removePropertiesFromChildrenExecutor, allChildrenHavePropertiesExecutor, iRBApprovalRequired,
				getAncestorInPCExecutor, getChildInPCExecutor, getChildrenInPCExecutor, getChildExecutor,
				getAncestorsInPCExecutor);
		//creating the pdp
		PDP pdp = new PDP(new PAP(graph, new MemProhibitions(), new MemObligations()), eppOptions);
		if (graph.exists("super_pc_rep")) {
			graph.deleteNode("super_pc_rep");
		}
		//adding obligations to the pdp through pap
		pdp.getPAP().getObligationsPAP().add(obligation, true);

		return pdp;
	}
	
}
