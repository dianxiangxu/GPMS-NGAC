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

		Set<Long> children = graph.getChildren(node.getID());
		Node[] nodes = graph.getNodes().toArray(new Node[graph.getNodes().size()]);
		for (long child : children) {
			Node childNode = graph.getNode(child);
			for (Node parentToBe : nodes) {
				Set<Long> childrentOfChild = graph.getChildren(child);

				if (parentToBe.getType() != UA || parentToBe.getID() == node.getID()
						|| childNode.getID() == parentToBe.getID() || childrentOfChild.contains(parentToBe.getID())) {
					continue;
				}

				Graph mutant = createCopy();

				changeAssignment(mutant, childNode.getID(), node.getID(), testMethod, testSuitePath, parentToBe);
				testMutant(mutant, testSuite, testMethod, getNumberOfMutants(), mutationMethod);
				setNumberOfMutants(getNumberOfMutants() + 1);
			}
		}

	}

	private void changeAssignment(Graph mutant, Long nodeForChange, Long nodeToRemoveAssociation, String testMethod,
			String testSuitePath, Node parentToBe) throws PMException, IOException {
		mutant.deassign(nodeForChange, nodeToRemoveAssociation);
		System.out.println("Child: " + graph.getNode(nodeForChange).getName() + " Parent: " + parentToBe.getName());
		mutant.assign(nodeForChange, parentToBe.getID());
	}

}
