package POMA.Mutation.ObligationMutationOperators;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import CaseStudies.gpms.Constants;
import CaseStudies.gpms.customEvents.*;
import CaseStudies.gpms.customFunctions.*;
import POMA.Utils;
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
import gov.nist.csd.pm.pip.obligations.model.Obligation;
import gov.nist.csd.pm.pip.prohibitions.MemProhibitions;

public class ObligationTestMutation {
	Graph graph = new MemGraph();
	Obligation obligation = new Obligation();

	
	@Before
	public void runBeforeEach() throws Exception {
		graph = MutantTester2.createCopy();
		obligation = MutantTester2.getObligationMutantCopy();
//		MuationFile = getFileFromResources("GPMSPolicies/GPMS/Obligations.yml"); InputStream is = new FileInputStream(obligationFile);obligation = EVRParser.parse(is);
	}
	
	@Test
	//a whole link from create proposal to chair approval
	public void allApprove() throws Exception {
		try {
			PReviewDecider decider = new PReviewDecider(graph);
			PDP pdp = getGPMSpdp(graph, obligation);
			Node target_copi = graph.getNode("CoPI");
			Node copiToAdd = graph.getNode("tomtom");
			Node target_sp = graph.getNode("SP");
			Node spToAdd = graph.getNode("vlad");

			//init proposal
			assertFalse(graph.getChildren("PI").contains("nazmul"));
			assertFalse(decider.check("nazmul", "", "PDSWhole", "Submit"));
			assertFalse(decider.check("nazmul", "", "PDSWhole", "Delete"));
			assertFalse(decider.check("nazmul", "", "PIEditable", "write"));
			assertFalse(decider.check("nazmul", "", "CoPI", "add-copi"));
			assertFalse(decider.check("nazmul", "", "CoPI", "delete-copi"));
			assertFalse(graph.getChildren("Chair").contains("ChairCSUser"));
			assertFalse(graph.getChildren("Business Manager").contains("bmCSUser"));
			assertFalse(graph.getChildren("Dean").contains("DeanCOEUser"));
			assertFalse(graph.getChildren("PIInfo").contains("nazmulPI"));
			pdp.getEPP().processEvent(new CreateEvent(graph.getNode(Constants.PDS_ORIGINATING_OA)), "nazmul", "process");
			assertTrue(graph.getChildren("PI").contains("nazmul"));
			assertTrue(decider.check("nazmul", "", "PDSWhole", "Submit"));
			assertTrue(decider.check("nazmul", "", "PDSWhole", "Delete"));
			assertTrue(decider.check("nazmul", "", "PIEditable", "write"));
			assertTrue(decider.check("nazmul", "", "CoPI", "add-copi"));
			assertTrue(decider.check("nazmul", "", "CoPI", "delete-copi"));
			assertTrue(graph.getChildren("Chair").contains("ChairCSUser"));
			assertTrue(graph.getChildren("Business Manager").contains("bmCSUser"));
			assertTrue(graph.getChildren("Dean").contains("DeanCOEUser"));
			assertTrue(graph.getChildren("PIInfo").contains("nazmulPI"));

			graph = getGPMSpdp(graph, obligation).getPAP().getGraphPAP();

			//add Copi
			assertFalse(decider.check("tomtom", "", "CoPIInfo", "read"));
			assertFalse(graph.getChildren("CoPI").contains("tomtom"));
			assertFalse(graph.getChildren("Chair").contains("ChairChemUser"));
			assertFalse(graph.getChildren("Business Manager").contains("bmChemUser"));
			assertFalse(graph.getChildren("Dean").contains("DeanCOASUser"));
			assertFalse(decider.check("tomtom", "", "CoPIEditable", "write"));
			assertFalse(graph.getChildren("CoPIInfo").contains("tomtomCoPI"));
			pdp.getEPP().processEvent(new AddCoPIEvent(target_copi, copiToAdd), "nazmul", "process");
			assertTrue(decider.check("tomtom", "", "CoPIInfo", "read"));
			assertTrue(graph.getChildren("CoPI").contains("tomtom"));
			assertTrue(graph.getChildren("Chair").contains("ChairChemUser"));//asser error
			assertTrue(graph.getChildren("Business Manager").contains("bmChemUser"));
			assertTrue(graph.getChildren("Dean").contains("DeanCOASUser"));
			assertTrue(decider.check("tomtom", "", "CoPIEditable", "read"));
			assertTrue(graph.getChildren("CoPIInfo").contains("tomtomCoPI"));

			//add SP
			assertFalse(graph.getChildren("SP").contains("vlad"));
			assertFalse(graph.getChildren("SPInfo").contains("vladSP"));
			pdp.getEPP().processEvent(new AddSPEvent(target_sp, spToAdd), "tomtom", "process");
			assertTrue(graph.getChildren("SP").contains("vlad"));
			assertTrue(graph.getChildren("SPInfo").contains("vladSP"));

			//submit proposal
			assertFalse(decider.check("ChairCSUser", "", "PDSSections", "read"));
			assertFalse(decider.check("ChairCSUser", "", "PDSWhole", "approve"));
			assertFalse(decider.check("ChairCSUser", "", "PDSWhole", "disapprove"));
			assertFalse(decider.check("ChairCSUser", "", "ChairApproval", "write"));
			assertFalse(decider.check("ChairChemUser", "", "PDSSections", "read"));
			assertFalse(decider.check("ChairChemUser", "", "PDSWhole", "approve"));
			assertFalse(decider.check("ChairChemUser", "", "PDSWhole", "disapprove"));
			assertFalse(decider.check("ChairChemUser", "", "ChairApproval", "write"));
			
			pdp.getEPP().processEvent(new SubmitEvent(graph.getNode("PDSWhole"), true),"nazmul", "process");
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
			//NEW
			assertFalse(decider.check("PI", "", "CoPI", "add-copi"));
			assertFalse(decider.check("PI", "", "CoPI", "delete-copi"));
//			assertFalse(decider.check("CoPI", "", "SP", "add-sp"));
//			assertFalse(decider.check("CoPI", "", "SP", "delete-sp"));
			assertTrue(decider.check("Chair", "", "PDSWhole", "Approve"));
			assertTrue(decider.check("Chair", "", "PDSWhole", "Disapprove"));
			
			//chair approve
			pdp.getEPP().processEvent(new ApproveEvent(graph.getNode(Constants.CHAIR_APPROVAL)), "ChairCSUser", "process");
			assertTrue(decider.check("ChairCSUser", "", "PDSWhole", "Approve"));
			assertTrue(decider.check("ChairCSUser", "", "PDSWhole", "Disapprove"));
			assertTrue(decider.check("ChairCSUser", "", "ChairApproval", "write"));
			assertTrue(decider.check("ChairChemUser", "", "PDSWhole", "Approve"));
			assertTrue(decider.check("ChairChemUser", "", "PDSWhole", "Disapprove"));
			assertTrue(decider.check("ChairChemUser", "", "ChairApproval", "write"));	
			
			assertFalse(decider.check("bmCSUser", "", "PDSSections", "read"));
			assertFalse(decider.check("bmCSUser", "", "PDSWhole", "Approve"));
			assertFalse(decider.check("bmCSUser", "", "PDSWhole", "Disapprove"));
			assertFalse(decider.check("bmCSUser", "", "BMApproval", "write"));
			assertFalse(decider.check("bmChemUser", "", "PDSSections", "read"));
			assertFalse(decider.check("bmChemUser", "", "PDSWhole", "Approve"));
			assertFalse(decider.check("bmChemUser", "", "PDSWhole", "Disapprove"));
			assertFalse(decider.check("bmChemUser", "", "BMApproval", "write"));
			
			assertFalse(decider.check("irbUser", "", "PDSSections", "read"));
			assertFalse(decider.check("irbUser", "", "PDSWhole", "Approve"));
			assertFalse(decider.check("irbUser", "", "PDSWhole", "Disapprove"));
			assertFalse(decider.check("irbUser", "", "IRBApproval", "write"));
			pdp.getEPP().processEvent(new ApproveEvent(graph.getNode(Constants.CHAIR_APPROVAL)), "ChairChemUser", "process");
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
			pdp.getEPP().processEvent(new ApproveEvent(graph.getNode(Constants.BM_APPROVAL)), "bmCSUser", "process");
			assertTrue(decider.check("bmCSUser", "", "PDSWhole", "Approve"));
			assertTrue(decider.check("bmCSUser", "", "PDSWhole", "Disapprove"));
			assertTrue(decider.check("bmCSUser", "", "BMApproval", "write"));
			assertTrue(decider.check("bmChemUser", "", "PDSWhole", "Approve"));
			assertTrue(decider.check("bmChemUser", "", "PDSWhole", "Disapprove"));
			assertTrue(decider.check("bmChemUser", "", "BMApproval", "write"));
			
			assertFalse(decider.check("DeanCOEUser", "", "PDSSections", "read"));
			assertFalse(decider.check("DeanCOEUser", "", "PDSWhole", "Approve"));
			assertFalse(decider.check("DeanCOEUser", "", "PDSWhole", "Disapprove"));
			assertFalse(decider.check("DeanCOEUser", "", "DeanApproval", "write"));
			assertFalse(decider.check("DeanCOASUser", "", "PDSSections", "read"));
			assertFalse(decider.check("DeanCOASUser", "", "PDSWhole", "Approve"));
			assertFalse(decider.check("DeanCOASUser", "", "PDSWhole", "Disapprove"));
			assertFalse(decider.check("DeanCOASUser", "", "DeanApproval", "write"));
			pdp.getEPP().processEvent(new ApproveEvent(graph.getNode(Constants.BM_APPROVAL)), "bmChemUser", "process");
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
			pdp.getEPP().processEvent(new ApproveEvent(graph.getNode(Constants.IRB_APPROVAL)),"irbUser", "process");
			assertFalse(decider.check("irbUser", "", "PDSWhole", "Approve"));
			assertFalse(decider.check("irbUser", "", "PDSWhole", "Disapprove"));
			assertFalse(decider.check("irbUser", "", "IRBApproval", "write"));
			
			//Dean approve
			pdp.getEPP().processEvent(new ApproveEvent(graph.getNode(Constants.DEAN_APPROVAL)), "DeanCOEUser", "process");
			assertFalse(decider.check("raUser", "", "PDSSections", "read"));
			assertFalse(decider.check("raUser", "", "PDSWhole", "Approve"));
			assertFalse(decider.check("raUser", "", "PDSWhole", "Disapprove"));
			assertFalse(decider.check("raUser", "", "PDSWhole", "Withdraw"));
			assertFalse(decider.check("raUser", "", "RAApproval", "write"));
			assertFalse(decider.check("raUser", "", "OSPInfo", "write"));
			//NEW
			assertTrue(decider.check("DeanCOEUser", "", "PDSWhole", "Approve"));
			assertTrue(decider.check("DeanCOEUser", "", "PDSWhole", "Disapprove"));
			assertTrue(decider.check("DeanCOEUser", "", "DeanApproval", "write"));
			assertTrue(decider.check("DeanCOASUser", "", "PDSWhole", "Approve"));
			assertTrue(decider.check("DeanCOASUser", "", "PDSWhole", "Disapprove"));
			assertTrue(decider.check("DeanCOASUser", "", "DeanApproval", "write"));
			pdp.getEPP().processEvent(new ApproveEvent(graph.getNode(Constants.DEAN_APPROVAL)), "DeanCOASUser", "process");
			assertTrue(decider.check("raUser", "", "PDSSections", "read"));
			assertTrue(decider.check("raUser", "", "PDSWhole", "Approve"));
			assertTrue(decider.check("raUser", "", "PDSWhole", "Disapprove"));
			assertTrue(decider.check("raUser", "", "PDSWhole", "Withdraw"));
			assertTrue(decider.check("raUser", "", "RAApproval", "write"));
			assertTrue(decider.check("raUser", "", "OSPInfo", "write"));
			//NEW
			assertFalse(decider.check("DeanCOEUser", "", "PDSWhole", "Approve"));
			assertFalse(decider.check("DeanCOEUser", "", "PDSWhole", "Disapprove"));
			assertFalse(decider.check("DeanCOEUser", "", "DeanApproval", "write"));
			assertFalse(decider.check("DeanCOASUser", "", "PDSWhole", "Approve"));
			assertFalse(decider.check("DeanCOASUser", "", "PDSWhole", "Disapprove"));
			assertFalse(decider.check("DeanCOASUser", "", "DeanApproval", "write"));
			
			//RA approve
			pdp.getEPP().processEvent(new ApproveEvent(graph.getNode(Constants.RA_APPROVAL)), "raUser", "process");
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
			assertTrue(decider.check("rdUser", "", "PDSSections", "read"));

			//NEW: below line is always false which means "grant RA write OSPInfo" never works
//			assertFalse(decider.check("Research Admin", "", "OSPInfo", "write"));
			//RD approve
			pdp.getEPP().processEvent(new ApproveEvent(graph.getNode(Constants.RD_APPROVAL)), "rdUser", "process");
			assertTrue(decider.check("raUser", "", "PDSWhole", "Submit"));
			assertTrue(decider.check("raUser", "", "RAApproval", "write"));
			
			assertFalse(decider.check("rdUser", "", "PDSWhole", "Approve"));
			assertFalse(decider.check("rdUser", "", "PDSWhole", "Disapprove"));
			assertFalse(decider.check("rdUser", "", "PDSWhole", "Delete"));
			assertFalse(decider.check("rdUser", "", "RDApproval", "write"));
			assertFalse(decider.check("rdUser", "", "OSPInfo", "write"));

			//RA submit
			pdp.getEPP().processEvent(new SubmitRAEvent(graph.getNode("PDSWhole")), "raUser", "process");
			assertFalse(decider.check("raUser", "", "PDSWhole", "Submit"));
			assertFalse(decider.check("raUser", "", "RAApproval", "write"));
			
			assertTrue(decider.check("rdUser", "", "PDSWhole", "Archive"));
			assertTrue(decider.check("rdUser", "", "RDApproval", "write"));
			//NEW
			assertFalse(decider.check("raUser", "", "OSPInfo", "write"));
			assertFalse(decider.check("Research Admin", "", "OSPInfo", "write"));
		} catch (PMException e) {
			e.printStackTrace();
		}
	}
	
	@Test
	//include delete_copi/delete_sp
	public void deleteCoPISP() throws Exception {
		try {
			PReviewDecider decider = new PReviewDecider(graph);
			Node target_copi = graph.getNode("CoPI");
			Node copiToAdd = graph.getNode("tomtom");
			Node target_sp = graph.getNode("SP");
			Node spToAdd = graph.getNode("vlad");
			
			//init proposal
			PDP pdp = getGPMSpdp(graph, obligation);
			pdp.getEPP().processEvent(new CreateEvent(graph.getNode(Constants.PDS_ORIGINATING_OA)), "nazmul", "process");
     		graph = getGPMSpdp(graph, obligation).getPAP().getGraphPAP();
     		
     		//add Copi				
			assertFalse(decider.check("tomtom", "", "CoPIInfo", "read"));
			assertFalse(graph.getChildren("CoPI").contains("tomtom"));
			assertFalse(graph.getChildren("Chair").contains("ChairChemUser"));
			assertFalse(graph.getChildren("Business Manager").contains("bmChemUser"));
			assertFalse(graph.getChildren("Dean").contains("DeanCOASUser"));
			assertFalse(decider.check("tomtom", "", "CoPIEditable", "write"));
			pdp.getEPP().processEvent(new AddCoPIEvent(target_copi, copiToAdd), "nazmul", "process");	
			assertTrue(decider.check("tomtom", "", "CoPIInfo", "read"));
			assertTrue(graph.getChildren("CoPI").contains("tomtom"));
			assertTrue(graph.getChildren("Chair").contains("ChairChemUser"));//assert error
			assertTrue(graph.getChildren("Business Manager").contains("bmChemUser"));
			assertTrue(graph.getChildren("Dean").contains("DeanCOASUser"));
			assertTrue(decider.check("tomtom", "", "CoPIEditable", "read"));
			
			//add SP
			assertFalse(graph.getChildren("SP").contains("vlad"));
			assertFalse(graph.getChildren("SPInfo").contains("vladSP"));
			pdp.getEPP().processEvent(new AddSPEvent(target_sp, spToAdd), "tomtom", "process");
			assertTrue(graph.getChildren("SP").contains("vlad"));
			assertTrue(graph.getChildren("SPInfo").contains("vladSP"));
			
			//delete SP
			pdp.getEPP().processEvent(new DeleteSPEvent(target_sp, spToAdd), "tomtom", "process");
			assertFalse(graph.getChildren("SP").contains("vlad"));
			assertFalse(graph.getChildren("SPInfo").contains("vladSP"));
			
			//delete CoPI
			pdp.getEPP().processEvent(new DeleteCoPIEvent(target_copi, copiToAdd), "nazmul", "process");
			assertFalse(decider.check("tomtom", "", "CoPIInfo", "read"));
			assertFalse(graph.getChildren("CoPI").contains("tomtom"));
			assertFalse(graph.getChildren("Chair").contains("ChairChemUser"));
			assertFalse(graph.getChildren("Business Manager").contains("bmChemUser"));
			assertFalse(graph.getChildren("Dean").contains("DeanCOASUser"));
			assertFalse(decider.check("tomtom", "", "CoPIEditable", "write"));
			assertFalse(graph.getChildren("CoPIInfo").contains("tomtomCoPI"));
     	} catch (PMException e) {
			e.printStackTrace();
		}
	}
	
	
	@Test
	public void chairDisapprove() throws Exception {
		try {
			PReviewDecider decider = new PReviewDecider(graph);
			
			//init proposal
			PDP pdp = getGPMSpdp(graph, obligation);
			pdp.getEPP().processEvent(new CreateEvent(graph.getNode(Constants.PDS_ORIGINATING_OA)), "nazmul", "process");
     		graph = getGPMSpdp(graph, obligation).getPAP().getGraphPAP();
     		
     		//add Copi and SP
			pdp.getEPP().processEvent(new AddCoPIEvent(graph.getNode("CoPI"), graph.getNode("tomtom")), "nazmul", "process");	
			pdp.getEPP().processEvent(new AddSPEvent(graph.getNode("SP"), graph.getNode("vlad")), "tomtom", "process");
			
			//submit proposal
			pdp.getEPP().processEvent(new SubmitEvent(graph.getNode("PDSWhole"), true),"nazmul", "process");
			
			//chair disapproval
//			pdp.getEPP().processEvent(new DisapproveEvent(graph.getNode(Constants.CHAIR_APPROVAL)), "ChairCSUser", "process");
			pdp.getEPP().processEvent(new DisapproveEvent(graph.getNode(Constants.CHAIR_APPROVAL)), "ChairChemUser", "process");
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
			//NEW
			assertFalse(decider.check("ChairCSUser", "", "PDSSections", "read"));
			assertFalse(decider.check("ChairChemUser", "", "PDSSections", "read"));
     	} catch (PMException e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void bmDisapprove() throws Exception {
		try {
			PReviewDecider decider = new PReviewDecider(graph);
		
			//init proposal
			PDP pdp = getGPMSpdp(graph, obligation);
			pdp.getEPP().processEvent(new CreateEvent(graph.getNode(Constants.PDS_ORIGINATING_OA)), "nazmul", "process");
			graph = getGPMSpdp(graph, obligation).getPAP().getGraphPAP();
			
			//add Copi and SP
			pdp.getEPP().processEvent(new AddCoPIEvent(graph.getNode("CoPI"), graph.getNode("tomtom")), "nazmul", "process");	
			pdp.getEPP().processEvent(new AddSPEvent(graph.getNode("SP"), graph.getNode("vlad")), "tomtom", "process");
			
			//submit proposal
			pdp.getEPP().processEvent(new SubmitEvent(graph.getNode("PDSWhole"), true),"nazmul", "process");

			//Chairs approve
			pdp.getEPP().processEvent(new ApproveEvent(graph.getNode(Constants.CHAIR_APPROVAL)), "ChairCSUser", "process");
			pdp.getEPP().processEvent(new ApproveEvent(graph.getNode(Constants.CHAIR_APPROVAL)), "ChairChemUser", "process");
			
			//step4: BMs disapprove
			pdp.getEPP().processEvent(new DisapproveEvent(graph.getNode(Constants.BM_APPROVAL)), "bmCSUser", "process");
//			pdp.getEPP().processEvent(new DisapproveEvent(graph.getNode(Constants.BM_APPROVAL)), "bmChemUser", "process");
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
	public void deanDisapprove() throws Exception {
		try {
			PReviewDecider decider = new PReviewDecider(graph);

			//init proposal
			PDP pdp = getGPMSpdp(graph, obligation);
			pdp.getEPP().processEvent(new CreateEvent(graph.getNode(Constants.PDS_ORIGINATING_OA)), "nazmul", "process");
			graph = getGPMSpdp(graph, obligation).getPAP().getGraphPAP();
 		
			//add Copi and SP
			pdp.getEPP().processEvent(new AddCoPIEvent(graph.getNode("CoPI"), graph.getNode("tomtom")), "nazmul", "process");	
			pdp.getEPP().processEvent(new AddSPEvent(graph.getNode("SP"), graph.getNode("vlad")), "tomtom", "process");
		
			//submit proposal
			pdp.getEPP().processEvent(new SubmitEvent(graph.getNode("PDSWhole"), true),"nazmul", "process");
		
			//Chairs approve
			pdp.getEPP().processEvent(new ApproveEvent(graph.getNode(Constants.CHAIR_APPROVAL)), "ChairCSUser", "process");
			pdp.getEPP().processEvent(new ApproveEvent(graph.getNode(Constants.CHAIR_APPROVAL)), "ChairChemUser", "process");

			//BMs approve
			pdp.getEPP().processEvent(new ApproveEvent(graph.getNode(Constants.BM_APPROVAL)), "bmCSUser", "process");
			pdp.getEPP().processEvent(new ApproveEvent(graph.getNode(Constants.BM_APPROVAL)), "bmChemUser", "process");

			//Deans disapprove
			pdp.getEPP().processEvent(new DisapproveEvent(graph.getNode(Constants.DEAN_APPROVAL)), "DeanCOEUser", "process");
//			pdp.getEPP().processEvent(new DisapproveEvent(graph.getNode(Constants.DEAN_APPROVAL)), "DeanCOAUser", "process");
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
			//NEW
			assertFalse(decider.check("bmCSUser", "", "PDSWhole", "Approve"));
			assertFalse(decider.check("bmCSUser", "", "PDSWhole", "Disapprove"));
			assertFalse(decider.check("bmCSUser", "", "BMApproval", "write"));
			assertFalse(decider.check("bmChemUser", "", "PDSWhole", "Approve"));
			assertFalse(decider.check("bmChemUser", "", "PDSWhole", "Disapprove"));
			assertFalse(decider.check("bmChemUser", "", "BMApproval", "write"));
     	} catch (PMException e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void irbDisapprove() throws Exception {
		try {
			PReviewDecider decider = new PReviewDecider(graph);

			//init proposal
			PDP pdp = getGPMSpdp(graph, obligation);
			pdp.getEPP().processEvent(new CreateEvent(graph.getNode(Constants.PDS_ORIGINATING_OA)), "nazmul", "process");
			graph = getGPMSpdp(graph, obligation).getPAP().getGraphPAP();
 		
			//add Copi and SP
			pdp.getEPP().processEvent(new AddCoPIEvent(graph.getNode("CoPI"), graph.getNode("tomtom")), "nazmul", "process");	
			pdp.getEPP().processEvent(new AddSPEvent(graph.getNode("SP"), graph.getNode("vlad")), "tomtom", "process");
		
			//submit proposal
			pdp.getEPP().processEvent(new SubmitEvent(graph.getNode("PDSWhole"), true),"nazmul", "process");
		
			//Chairs approve
			pdp.getEPP().processEvent(new ApproveEvent(graph.getNode(Constants.CHAIR_APPROVAL)), "ChairCSUser", "process");
			pdp.getEPP().processEvent(new ApproveEvent(graph.getNode(Constants.CHAIR_APPROVAL)), "ChairChemUser", "process");

			//BMs approve
			pdp.getEPP().processEvent(new ApproveEvent(graph.getNode(Constants.BM_APPROVAL)), "bmCSUser", "process");
			pdp.getEPP().processEvent(new ApproveEvent(graph.getNode(Constants.BM_APPROVAL)), "bmChemUser", "process");
			
			//Deans approve
			pdp.getEPP().processEvent(new ApproveEvent(graph.getNode(Constants.DEAN_APPROVAL)), "DeanCOEUser", "process");
			pdp.getEPP().processEvent(new ApproveEvent(graph.getNode(Constants.DEAN_APPROVAL)), "DeanCOASUser", "process");
			
			//IRB disapprove
			pdp.getEPP().processEvent(new DisapproveEvent(graph.getNode(Constants.IRB_APPROVAL)), "irbUser", "process");
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
			//NEW
			assertFalse(decider.check("bmCSUser", "", "PDSWhole", "Approve"));
			assertFalse(decider.check("bmCSUser", "", "PDSWhole", "Disapprove"));
			assertFalse(decider.check("bmCSUser", "", "BMApproval", "write"));
			assertFalse(decider.check("bmChemUser", "", "PDSWhole", "Approve"));
			assertFalse(decider.check("bmChemUser", "", "PDSWhole", "Disapprove"));
			assertFalse(decider.check("bmChemUser", "", "BMApproval", "write"));
     	} catch (PMException e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void raDisapprove() throws Exception {
		try {
			PReviewDecider decider = new PReviewDecider(graph);

			//init proposal
			PDP pdp = getGPMSpdp(graph, obligation);
			pdp.getEPP().processEvent(new CreateEvent(graph.getNode(Constants.PDS_ORIGINATING_OA)), "nazmul", "process");
			graph = getGPMSpdp(graph, obligation).getPAP().getGraphPAP();
 		
			//add Copi and SP
			pdp.getEPP().processEvent(new AddCoPIEvent(graph.getNode("CoPI"), graph.getNode("tomtom")), "nazmul", "process");	
			pdp.getEPP().processEvent(new AddSPEvent(graph.getNode("SP"), graph.getNode("vlad")), "tomtom", "process");
		
			//submit proposal
			pdp.getEPP().processEvent(new SubmitEvent(graph.getNode("PDSWhole"), true),"nazmul", "process");
		
			//Chairs approve
			pdp.getEPP().processEvent(new ApproveEvent(graph.getNode(Constants.CHAIR_APPROVAL)), "ChairCSUser", "process");
			pdp.getEPP().processEvent(new ApproveEvent(graph.getNode(Constants.CHAIR_APPROVAL)), "ChairChemUser", "process");

			//BMs approve
			pdp.getEPP().processEvent(new ApproveEvent(graph.getNode(Constants.BM_APPROVAL)), "bmCSUser", "process");
			pdp.getEPP().processEvent(new ApproveEvent(graph.getNode(Constants.BM_APPROVAL)), "bmChemUser", "process");
			
			//Deans approve
			pdp.getEPP().processEvent(new ApproveEvent(graph.getNode(Constants.DEAN_APPROVAL)), "DeanCOEUser", "process");
			pdp.getEPP().processEvent(new ApproveEvent(graph.getNode(Constants.DEAN_APPROVAL)), "DeanCOASUser", "process");
			
			//IRB approve
			pdp.getEPP().processEvent(new ApproveEvent(graph.getNode(Constants.IRB_APPROVAL)),"irbUser", "process");
			
			//RA disapprove
			pdp.getEPP().processEvent(new DisapproveEvent(graph.getNode(Constants.RA_APPROVAL)), "raUser", "process");			
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
	public void rdDisapprove() throws Exception {
		try {
			PReviewDecider decider = new PReviewDecider(graph);

			//init proposal
			PDP pdp = getGPMSpdp(graph, obligation);
			pdp.getEPP().processEvent(new CreateEvent(graph.getNode(Constants.PDS_ORIGINATING_OA)), "nazmul", "process");
			graph = getGPMSpdp(graph, obligation).getPAP().getGraphPAP();
 		
			//add Copi and SP
			pdp.getEPP().processEvent(new AddCoPIEvent(graph.getNode("CoPI"), graph.getNode("tomtom")), "nazmul", "process");	
			pdp.getEPP().processEvent(new AddSPEvent(graph.getNode("SP"), graph.getNode("vlad")), "tomtom", "process");
		
			//submit proposal
			pdp.getEPP().processEvent(new SubmitEvent(graph.getNode("PDSWhole"), true),"nazmul", "process");
		
			//Chairs approve
			pdp.getEPP().processEvent(new ApproveEvent(graph.getNode(Constants.CHAIR_APPROVAL)), "ChairCSUser", "process");
			pdp.getEPP().processEvent(new ApproveEvent(graph.getNode(Constants.CHAIR_APPROVAL)), "ChairChemUser", "process");

			//BMs approve
			pdp.getEPP().processEvent(new ApproveEvent(graph.getNode(Constants.BM_APPROVAL)), "bmCSUser", "process");
			pdp.getEPP().processEvent(new ApproveEvent(graph.getNode(Constants.BM_APPROVAL)), "bmChemUser", "process");
			
			//Deans approve
			pdp.getEPP().processEvent(new ApproveEvent(graph.getNode(Constants.DEAN_APPROVAL)), "DeanCOEUser", "process");
			pdp.getEPP().processEvent(new ApproveEvent(graph.getNode(Constants.DEAN_APPROVAL)), "DeanCOASUser", "process");
			
			//IRB approve
			pdp.getEPP().processEvent(new ApproveEvent(graph.getNode(Constants.IRB_APPROVAL)),"irbUser", "process");
			
			//RA approve
			pdp.getEPP().processEvent(new ApproveEvent(graph.getNode(Constants.RA_APPROVAL)), "raUser", "process");
			
			//RD disapprove
			pdp.getEPP().processEvent(new DisapproveEvent(graph.getNode(Constants.RD_APPROVAL)), "rdUser", "process");
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

	
	public static File getFileFromResources(String fileName) {
		File resource = new File(fileName);
		return resource;
	}
	
	// the method to get pdp and load it with custom functions, graph, and obligations. 
	public static PDP getGPMSpdp(Graph graph, Obligation obligation) throws Exception {
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
		if (graph.exists("super_pc_rep")) {
			graph.deleteNode("super_pc_rep");
		}
		//adding obligations to the pdp through pap
		pdp.getPAP().getObligationsPAP().add(obligation, true);		
		return pdp;
	}
    
}