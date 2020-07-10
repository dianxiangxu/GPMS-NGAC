package gpms.ngac.GraphTester;

import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.OA;
import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.PC;
import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.U;
import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.UA;
import gov.nist.csd.pm.pdp.decider.PReviewDecider;
import gov.nist.csd.pm.exceptions.PMException;
import gov.nist.csd.pm.operations.OperationSet;
import gov.nist.csd.pm.pip.graph.Graph;
import gov.nist.csd.pm.pip.graph.MemGraph;
import gov.nist.csd.pm.pip.graph.model.nodes.Node;
import gpms.model.GPMSCommonInfo;
import gpms.ngac.policy.Attribute;
import gpms.ngac.policy.NGACPolicyConfigurationLoader;
import gpms.ngac.policy.Task;
import gpms.ngac.policy.UserPermissionChecker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Stack;

public abstract class GraphTesterPairwise {
	Graph graph;
	List<String> users;
	List<String> UAs;
	Node policy;
	Node createPDS;
	Node writePDS;
	Node readPDS;
	List<Node> OAs = new ArrayList<Node>();
	List<Node> PCs = new ArrayList<Node>();
	public GraphTesterPairwise() throws PMException, InterruptedException{
		
		graph = buildGraphGPMS1();
	}
	private Graph buildGraphGPMS1() throws PMException, InterruptedException {
		graph = new MemGraph();
		policy = graph.createPolicyClass("ProposalCreation", null);
		PCs.add(policy);

		OperationSet operationsPI = new OperationSet();
		operationsPI.add("create-oa");
		operationsPI.add("create-oa-to-oa");
		
		OperationSet operationsNon = new OperationSet();
		operationsNon.add("write");
		operationsNon.add("read");
		createPDS = graph.createNode("createPDS", OA, null,"ProposalCreation");
		writePDS = graph.createNode("writePDS", OA, null, "ProposalCreation");
		readPDS = graph.createNode("readPDS", OA, null, "ProposalCreation");
		OAs.add(createPDS);
		OAs.add(writePDS);
		OAs.add(readPDS);
		Node facultyUA = graph.createNode("faculty", UA, null,"ProposalCreation");

		Node nonTenureTrackUA = graph.createNode("non-tenure-track",
				UA, null,"faculty");
		Node adjunctUA = graph.createNode("adjunct", UA, null,"faculty" );
		Node clinicalUA = graph.createNode( "clinical", UA, null,"non-tenure-track");
		Node teachingUA = graph.createNode("teaching", UA, null, "non-tenure-track");
		Node researchUA = graph.createNode("research", UA, null, "non-tenure-track");
		Node tenureTrackTenuredUA = graph.createNode(
				"tenureTrackTenured", UA, null, "faculty");
		Node tenureTrackUA = graph
				.createNode( "tenure-track", UA, null,"tenureTrackTenured");
		Node tenuredUA = graph.createNode("tenured", UA, null, "tenureTrackTenured");



		Node SPElegibleUA = graph.createNode( "SPElegible", UA, null, "ProposalCreation");
		graph.assign(nonTenureTrackUA.getName(), SPElegibleUA.getName());
		graph.assign(tenureTrackTenuredUA.getName(), SPElegibleUA.getName());

		Node CoPIElegibleUA = graph.createNode( "CoPIElegible", UA,
				null, "ProposalCreation");
		graph.assign(nonTenureTrackUA.getName(), CoPIElegibleUA.getName());
		graph.assign(tenureTrackTenuredUA.getName(), CoPIElegibleUA.getName());

		Node PIElegibleUA = graph.createNode("PIElegible", UA, null,"ProposalCreation");
		graph.assign(researchUA.getName(), PIElegibleUA.getName());
		graph.assign(tenureTrackTenuredUA.getName(), PIElegibleUA.getName());


		graph.assign(PIElegibleUA.getName(), policy.getName());

		graph.associate(PIElegibleUA.getName(), createPDS.getName(), operationsPI);
		
		graph.associate(facultyUA.getName(), writePDS.getName(), operationsNon);
	
		
		graph.associate(tenuredUA.getName(), readPDS.getName(), operationsNon);
	

		users = new ArrayList<String>();
		users.add(tenuredUA.getName());
		users.add(tenureTrackUA.getName());
		users.add(researchUA.getName());
		users.add(teachingUA.getName());
		users.add(clinicalUA.getName());
		users.add(adjunctUA.getName());
		users.add(PIElegibleUA.getName());




		return graph;
	}
	public static Random rand = new Random();

	public static long getID() {
		return rand.nextLong();
	}
	public Graph getGraph(){
		return graph;
	}
	public List<String> getUsers(){
		return users;
	}
	public void testGraphPC() throws PMException {
	}
	public void testGraphPC(List<String> list) throws PMException {
		
	}
	public List<String[]> testGraphPairwisePC() throws PMException {
		return null;
	}
	
}
