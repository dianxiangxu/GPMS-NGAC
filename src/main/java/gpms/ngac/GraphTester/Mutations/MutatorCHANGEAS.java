package gpms.ngac.GraphTester.Mutations;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import gov.nist.csd.pm.exceptions.PMException;
import gov.nist.csd.pm.pip.graph.Graph;
import gov.nist.csd.pm.pip.graph.model.nodes.Node;
import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.UA;

public class MutatorCHANGEAS extends MutantTester {
	String mutationMethod = "CHANGEAS";

	public void init(String testMethod) throws PMException, IOException {
		String testResults = "CSV/" + testMethod + "/" + testMethod + "testResultsCHANGEAS.csv";
		String testSuitePath = "CSV/testSuits/" + testMethod + "testSuite.csv";
		getGraphLoaded(initialGraphConfig);
		for (Node node : UAs) {
			performMutation(node, testMethod, testSuitePath);
		}
		saveCSV(data, new File(testResults), testMethod);

	}

	private void performMutation(Node node, String testMethod, String testSuitePath) throws PMException, IOException {
		File testSuite = new File(testSuitePath);

		Set<String> children = graph.getChildren(node.getName());
		Node[] nodes = graph.getNodes().toArray(new Node[graph.getNodes().size()]);
		for (String child : children) {
			Node childNode = graph.getNode(child);
			for (Node parentToBe : nodes) {
				Set<String> childrentOfChild = graph.getChildren(child);

				if (parentToBe.getType() != UA || parentToBe.getName().equals(node.getName())
						|| childNode.getName().equals(parentToBe.getName()) || childrentOfChild.contains(parentToBe.getName())) {
					continue;
				}

				Graph mutant = createCopy();

				changeAssignment(mutant, childNode.getName(), node.getName(), testMethod, testSuitePath, parentToBe);
				testMutant(mutant, testSuite, testMethod, getNumberOfMutants(), mutationMethod);
				setNumberOfMutants(getNumberOfMutants() + 1);
			}
		}

	}

	private void changeAssignment(Graph mutant, String nodeForChange, String nodeToRemoveAssociation, String testMethod,
			String testSuitePath, Node parentToBe) throws PMException, IOException {
		mutant.deassign(nodeForChange, nodeToRemoveAssociation);
		//System.out.println("Child: " + graph.getNode(nodeForChange).getName() + " Parent: " + parentToBe.getName());
		mutant.assign(nodeForChange, parentToBe.getName());
	}

}
