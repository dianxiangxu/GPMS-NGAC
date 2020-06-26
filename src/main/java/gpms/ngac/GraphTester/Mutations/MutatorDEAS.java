package gpms.ngac.GraphTester.Mutations;

import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.OA;
import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.UA;
import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.PC;

import gov.nist.csd.pm.exceptions.PMException;
import gov.nist.csd.pm.pdp.decider.PReviewDecider;
import gov.nist.csd.pm.pip.graph.Graph;
import gov.nist.csd.pm.pip.graph.GraphSerializer;
import gov.nist.csd.pm.pip.graph.MemGraph;
import gov.nist.csd.pm.pip.graph.model.nodes.Node;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

public class MutatorDEAS extends MutantTester {
	String mutationMethod = "DEAS";

	public void init(String testMethod) throws PMException, IOException {
		String testResults = "CSV/" + testMethod + "/" + testMethod + "testResultsDEAS.csv";
		String testSuitePath = "CSV/testSuits/" + testMethod + "testSuite.csv";
		getGraphLoaded(initialGraphConfig);
		for (Node node : UAsPCs) {
			performMutation(node, testMethod, testSuitePath);
		}
		saveCSV(data, new File(testResults), testMethod);

	}

	private void performMutation(Node node, String testMethod, String testSuitePath) throws PMException, IOException {

		File testSuite = new File(testSuitePath);

		Set<Long> children = graph.getChildren(node.getID());
		for (long child : children) {
			Graph mutant = createCopy();
			Node childNode = mutant.getNode(child);
			mutant.deassign(childNode.getID(), node.getID());
			testMutant(mutant, testSuite, testMethod, getNumberOfMutants(), mutationMethod);
			setNumberOfMutants(getNumberOfMutants() + 1);

		}

	}

	

	private void testCase0() throws PMException {
		PReviewDecider decider = new PReviewDecider(graph);
		String[] requiredAccessRights1 = { "create-oa" };
		Set<Node> set1 = graph.search("PIElegible", "UA", null);
		Node UA = set1.iterator().next();
		Set<Node> set2 = graph.search("createPDS", "OA", null);
		Node OA = set2.iterator().next();
		Set<Node> set3 = graph.search("ProposalCreation", "PC", null);
		Node PC = set3.iterator().next();
		//System.out.println(
				"Test on the original graph: " + decider.check(UA.getID(), 101L, OA.getID(), requiredAccessRights1));

	}

	private void testCase1() throws PMException {
		Graph mutant = new MemGraph();
		String json = GraphSerializer.toJson(graph);
		mutant = GraphSerializer.fromJson(new MemGraph(), json);
		String[] requiredAccessRights1 = { "create-oa" };
		Set<Node> set1 = mutant.search("PIElegible", "UA", null);
		Node UA = set1.iterator().next();
		Set<Node> set2 = mutant.search("createPDS", "OA", null);
		Node OA = set2.iterator().next();
		Set<Node> set3 = mutant.search("ProposalCreation", "PC", null);
		Node PC = set3.iterator().next();

		PReviewDecider decider = new PReviewDecider(mutant);
		//System.out.println("Test on the graph before removing the assignment of UA to PC: "
				+ decider.check(UA.getID(), 101L, OA.getID(), requiredAccessRights1));
		mutant.deassign(UA.getID(), PC.getID());
		//System.out.println("Test on the graph after removing the assignment of UA to PC: "
				+ decider.check(UA.getID(), 101L, OA.getID(), requiredAccessRights1));

	}

	private void testCase2() throws PMException {
		Graph mutant = new MemGraph();
		String json = GraphSerializer.toJson(graph);
		mutant = GraphSerializer.fromJson(new MemGraph(), json);
		String[] requiredAccessRights1 = { "create-oa" };
		Set<Node> set1 = mutant.search("PIElegible", "UA", null);
		Node UA = set1.iterator().next();
		Set<Node> set2 = mutant.search("createPDS", "OA", null);
		Node OA = set2.iterator().next();
		Set<Node> set3 = mutant.search("ProposalCreation", "PC", null);
		Node PC = set3.iterator().next();

		PReviewDecider decider = new PReviewDecider(mutant);
		//System.out.println("Test on the graph before removing the assignment of OA to PC: "
				+ decider.check(UA.getID(), 101L, OA.getID(), requiredAccessRights1));
		mutant.deassign(OA.getID(), PC.getID());
		//System.out.println("Test on the graph after removing the assignment of OA to PC: "
				+ decider.check(UA.getID(), 101L, OA.getID(), requiredAccessRights1));

	}
}
