package gpms.ngac.GraphTester.Mutations;

import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.OA;
import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.PC;
import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.UA;
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
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

public class MutatorREMNODE extends MutantTester{
	String mutationMethod = "REMNODE";

	public void init(String testMethod) throws PMException, IOException {
		String testResults = "CSV/"+testMethod+"/"+testMethod+"testResultsREMNODE.csv";
		String testSuitePath = "CSV/testSuits/"+testMethod+"testSuite.csv";
		File testResultsFile = new File(testResults);
		getGraphLoaded(initialGraphConfig);

		performMutation(testMethod, testSuitePath);
		saveCSV(data, testResultsFile, testMethod);		

	}
	private void performMutation(String testMethod, String testSuitePath) throws PMException, IOException {
		File testSuite = new File(testSuitePath);
		int mutantNumber = 0;

		for (Node elegibleNode : UAsPCsOAs) {
			Graph mutant = createCopy();

			mutant.deleteNode(elegibleNode.getID());
			testMutant(mutant,testSuite , testMethod, getNumberOfMutants(), mutationMethod);
			setNumberOfMutants(getNumberOfMutants() + 1);
		}
		
	}


}
