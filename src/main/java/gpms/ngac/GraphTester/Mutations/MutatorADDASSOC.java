package gpms.ngac.GraphTester.Mutations;

import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.OA;
import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.U;
import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.UA;

import gov.nist.csd.pm.exceptions.PMException;
import gov.nist.csd.pm.operations.OperationSet;
import gov.nist.csd.pm.pdp.decider.PReviewDecider;
import gov.nist.csd.pm.pip.graph.Graph;
import gov.nist.csd.pm.pip.graph.GraphSerializer;
import gov.nist.csd.pm.pip.graph.MemGraph;
import gov.nist.csd.pm.pip.graph.model.nodes.Node;
import gpms.ngac.policy.PDSOperations;

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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

public class MutatorADDASSOC extends MutantTester {
	String mutationMethod = "ADDASSOC";

	public void init(String testMethod) throws PMException, IOException {
		String testResults = "CSV/"+testMethod+"/"+testMethod+"testResultsADDASSOC.csv";
		String testSuitePath = "CSV/testSuits/"+testMethod+"testSuite.csv";
		File testResultsFile = new File(testResults);
		getGraphLoaded(initialGraphConfig);
		performMutation(testMethod, testSuitePath);
		saveCSV(data, testResultsFile, testMethod);

	}
	
	private void performMutation(String testMethod, String testSuitePath) throws PMException, IOException {
		File testSuite = new File(testSuitePath);
		for (Node ua : UAs) {
			Graph mutant = createCopy();
			Node oa = OAs.get(0);
			mutant = associate(mutant,oa,ua.getName());
			testMutant(mutant,testSuite , testMethod, getNumberOfMutants(), mutationMethod);
			setNumberOfMutants(getNumberOfMutants() + 1);
		}		


		
	}
	
	private Graph associate(Graph mutant, Node oa, String uaID) throws PMException {
		OperationSet operationsPI = new OperationSet();
		operationsPI.add("create-oa");
		operationsPI.add("create-oa-to-oa");
		mutant.associate(uaID, oa.getName(), operationsPI);
		return mutant;
	}
}
