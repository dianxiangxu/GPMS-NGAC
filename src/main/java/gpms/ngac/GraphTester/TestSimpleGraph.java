package gpms.ngac.GraphTester;

import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.*;

import java.util.Map;

import gov.nist.csd.pm.exceptions.PMException;
import gov.nist.csd.pm.operations.OperationSet;
import gov.nist.csd.pm.pdp.decider.PReviewDecider;
import gov.nist.csd.pm.pip.graph.Graph;
import gov.nist.csd.pm.pip.graph.MemGraph;
import gov.nist.csd.pm.pip.graph.model.nodes.Node;

public class TestSimpleGraph {
	public static void main(String[] args) throws Exception {
		TestSimpleGraph tsg = new TestSimpleGraph(); 
		tsg.buildSimpleGraph();
	}
	private Graph buildSimpleGraph() throws PMException, InterruptedException {
		Graph graph = new MemGraph();
		Node policy = graph.createPolicyClass("pc1", null);
		Node ua3 = graph.createNode("ua3", UA, null, "pc1");
		Node ua2 = graph.createNode("ua2", UA, null, "pc1");
		Node ua1 = graph.createNode("ua1", UA, null, "ua2");
		Node u1 = graph.createNode("u1", UA, null, "ua1");
		Node u2 = graph.createNode("u2", UA, null, "ua2");
		Node u3 = graph.createNode("u3", UA, null, "ua3");

		Node oa2 = graph.createNode("oa2", UA, null, "pc1");
		Node oa1 = graph.createNode("oa1", UA, null, "oa2");
		Node o1 = graph.createNode("o1", UA, null, "oa1");
		Node o2 = graph.createNode("o2", UA, null, "oa2");

		graph.associate(ua1.getName(), oa1.getName(), new OperationSet("w"));
		graph.associate(ua2.getName(), oa2.getName(), new OperationSet("r", "x"));
		graph.associate(ua1.getName(), ua3.getName(), new OperationSet("add"));

		PReviewDecider decider = new PReviewDecider(graph, null);

		for (String s : decider.list(ua2.getName(), "", oa2.getName())) {

			System.out.println("UA2 AND OA2: " + s);
		}
		System.out.println("------------------------------------------------------");

		for (String s : decider.list(ua1.getName(), "", oa2.getName())) {

			System.out.println("UA1 AND OA2: " + s);
		}
		System.out.println("------------------------------------------------------");
		
		for (String s : decider.list(ua2.getName(), "", oa1.getName())) {

			System.out.println("UA2 AND OA1: " + s);
		}
		System.out.println("------------------------------------------------------");
		
		for (String s : decider.list(ua1.getName(), "", oa1.getName())) {

			System.out.println("UA1 AND OA1: " + s);
		}
		System.out.println("------------------------------------------------------");
		
		Map<String, OperationSet> map = graph.getSourceAssociations("ua1");
		
		for (Map.Entry<String,OperationSet> entry : map.entrySet())  
            System.out.println("Key = " + entry.getKey() + 
                             ", Value = " + entry.getValue()); 
		
		
		return graph;
	}

	
	
}