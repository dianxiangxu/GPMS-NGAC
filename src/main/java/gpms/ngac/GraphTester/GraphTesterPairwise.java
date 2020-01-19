package gpms.ngac.GraphTester;

import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.OA;
import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.PC;
import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.U;
import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.UA;
import gov.nist.csd.pm.pdp.decider.PReviewDecider;
import gov.nist.csd.pm.exceptions.PMException;
import gov.nist.csd.pm.pip.graph.Graph;
import gov.nist.csd.pm.pip.graph.MemGraph;
import gov.nist.csd.pm.pip.graph.model.nodes.Node;
import gpms.dev.PDS;
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
	List<Long> users;
	List<Long> UAs;
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
		policy = graph.createNode(getID(), "ProposalCreation", PC, null);
		PCs.add(policy);

		Set<String> operationsPI = new HashSet<String>();
		operationsPI.add("create-oa");
		operationsPI.add("create-oa-to-oa");
		
		Set<String> operationsNon = new HashSet<String>();
		operationsNon.add("write");
		operationsNon.add("read");
		createPDS = graph.createNode(getID(), "createPDS", OA, null);
		writePDS = graph.createNode(getID(), "writePDS", OA, null);
		readPDS = graph.createNode(getID(), "readPDS", OA, null);
		OAs.add(createPDS);
		OAs.add(writePDS);
		OAs.add(readPDS);
		
		graph.assign(createPDS.getID(), policy.getID());
		graph.assign(writePDS.getID(), policy.getID());
		graph.assign(readPDS.getID(), policy.getID());


		Node adjunctUA = graph.createNode(getID(), "adjunct", UA, null);
		Node clinicalUA = graph.createNode(getID(), "clinical", UA, null);
		Node teachingUA = graph.createNode(getID(), "teaching", UA, null);
		Node researchUA = graph.createNode(getID(), "research", UA, null);
		Node tenureTrackUA = graph
				.createNode(getID(), "tenure-track", UA, null);
		Node tenuredUA = graph.createNode(getID(), "tenured", UA, null);

		Node nonTenureTrackUA = graph.createNode(getID(), "non-tenure-track",
				UA, null);
		graph.assign(clinicalUA.getID(), nonTenureTrackUA.getID());
		graph.assign(teachingUA.getID(), nonTenureTrackUA.getID());
		graph.assign(researchUA.getID(), nonTenureTrackUA.getID());

		Node tenureTrackTenuredUA = graph.createNode(getID(),
				"tenureTrackTenured", UA, null);
		graph.assign(tenureTrackUA.getID(), tenureTrackTenuredUA.getID());
		graph.assign(tenuredUA.getID(), tenureTrackTenuredUA.getID());

		Node facultyUA = graph.createNode(getID(), "faculty", UA, null);
		graph.assign(adjunctUA.getID(), facultyUA.getID());
		graph.assign(nonTenureTrackUA.getID(), facultyUA.getID());
		graph.assign(tenureTrackTenuredUA.getID(), facultyUA.getID());

		Node SPElegibleUA = graph.createNode(getID(), "SPElegible", UA, null);
		graph.assign(nonTenureTrackUA.getID(), SPElegibleUA.getID());
		graph.assign(tenureTrackTenuredUA.getID(), SPElegibleUA.getID());

		Node CoPIElegibleUA = graph.createNode(getID(), "CoPIElegible", UA,
				null);
		graph.assign(nonTenureTrackUA.getID(), CoPIElegibleUA.getID());
		graph.assign(tenureTrackTenuredUA.getID(), CoPIElegibleUA.getID());

		Node PIElegibleUA = graph.createNode(getID(), "PIElegible", UA, null);
		graph.assign(researchUA.getID(), PIElegibleUA.getID());
		graph.assign(tenureTrackTenuredUA.getID(), PIElegibleUA.getID());

		graph.assign(facultyUA.getID(), policy.getID());
		graph.assign(SPElegibleUA.getID(), policy.getID());
		graph.assign(CoPIElegibleUA.getID(), policy.getID());
		graph.assign(PIElegibleUA.getID(), policy.getID());

		graph.associate(PIElegibleUA.getID(), createPDS.getID(), operationsPI);
		
		graph.associate(facultyUA.getID(), writePDS.getID(), operationsNon);
	
		
		graph.associate(tenuredUA.getID(), readPDS.getID(), operationsNon);
	

		users = new ArrayList<Long>();
		users.add(tenuredUA.getID());
		users.add(tenureTrackUA.getID());
		users.add(researchUA.getID());
		users.add(teachingUA.getID());
		users.add(clinicalUA.getID());
		users.add(adjunctUA.getID());
		users.add(PIElegibleUA.getID());




		return graph;
	}
	public static Random rand = new Random();

	public static long getID() {
		return rand.nextLong();
	}
	public Graph getGraph(){
		return graph;
	}
	public List<Long> getUsers(){
		return users;
	}
	public void testGraphPC() throws PMException {
	}
	public void testGraphPC(List<Long> list) throws PMException {
		
	}
	public List<String[]> testGraphPairwisePC() throws PMException {
		return null;
	}
	
}
