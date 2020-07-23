package gpms.policy.GraphTester.Mutations;

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

		Set<String> children = graph.getChildren(node.getName());
		for (String child : children) {
			Graph mutant = createCopy();
			Node childNode = mutant.getNode(child);
			mutant.deassign(childNode.getName(), node.getName());
			testMutant(mutant, testSuite, testMethod, getNumberOfMutants(), mutationMethod);
			setNumberOfMutants(getNumberOfMutants() + 1);

		}

	}

	

	private void testCase0() throws PMException {
		PReviewDecider decider = new PReviewDecider(graph);
		String[] requiredAccessRights1 = { "create-oa" };
		Node UA  = graph.getNode("PIElegible");
		Node OA = graph.getNode("createPDS");
		Node PC = graph.getNode("ProposalCreation");
		//System.out.println(
			//	"Test on the original graph: " + decider.check(UA.getID(), 101L, OA.getID(), requiredAccessRights1));

	}

	private void testCase1() throws PMException {
		Graph mutant = new MemGraph();
		String json = GraphSerializer.toJson(graph);
		GraphSerializer.fromJson(mutant, json);
		String[] requiredAccessRights1 = { "create-oa" };
		Node UA  = graph.getNode("PIElegible");
		Node OA = graph.getNode("createPDS");
		Node PC = graph.getNode("ProposalCreation");

		PReviewDecider decider = new PReviewDecider(mutant);
		//System.out.println("Test on the graph before removing the assignment of UA to PC: "
				//+ decider.check(UA.getID(), 101L, OA.getID(), requiredAccessRights1));
		mutant.deassign(UA.getName(), PC.getName());
		//System.out.println("Test on the graph after removing the assignment of UA to PC: "
				//+ decider.check(UA.getID(), 101L, OA.getID(), requiredAccessRights1));

	}

	private void testCase2() throws PMException {
		Graph mutant = new MemGraph();
		String json = GraphSerializer.toJson(graph);
		GraphSerializer.fromJson(mutant, json);
		String[] requiredAccessRights1 = { "create-oa" };
		Node UA  = graph.getNode("PIElegible");
		Node OA = graph.getNode("createPDS");
		Node PC = graph.getNode("ProposalCreation");

		PReviewDecider decider = new PReviewDecider(mutant);
		//System.out.println("Test on the graph before removing the assignment of OA to PC: "
			//	+ decider.check(UA.getID(), 101L, OA.getID(), requiredAccessRights1));
		mutant.deassign(OA.getName(), PC.getName());
		//System.out.println("Test on the graph after removing the assignment of OA to PC: "
			//	+ decider.check(UA.getID(), 101L, OA.getID(), requiredAccessRights1));

	}
}
