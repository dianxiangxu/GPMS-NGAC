package gpms.ngac.GraphTester.Mutations;

import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.OA;
import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.UA;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

import gov.nist.csd.pm.exceptions.PMException;
import gov.nist.csd.pm.pdp.decider.PReviewDecider;
import gov.nist.csd.pm.pip.graph.Graph;
import gov.nist.csd.pm.pip.graph.GraphSerializer;
import gov.nist.csd.pm.pip.graph.MemGraph;
import gov.nist.csd.pm.pip.graph.model.nodes.Node;

public class MutatorREMAR extends MutantTester {
	String mutationMethod = "REMAR";

	public void init(String testMethod) throws PMException, IOException {
		String testResults = "CSV/" + testMethod + "/" + testMethod + "testResultsREMAR.csv";
		String testSuitePath = "CSV/testSuits/" + testMethod + "testSuite.csv";
		getGraphLoaded(initialGraphConfig);
		performMutation( testMethod,  testSuitePath);
		saveCSV(data, new File(testResults), testMethod);

	}

	private void performMutation(String testMethod, String testSuitePath) throws PMException, IOException {
		

		File testSuite = new File(testSuitePath);

		for (Node oa : OAs) {
			if (graph.getTargetAssociations(oa.getID()) != null) {
				Long oaID = oa.getID();
				removeAccessRight(oaID, testMethod, testSuite);
			}
		}

	}

	private void removeAccessRight(Long oaID, String testMethod, File testSuite) throws PMException, IOException {

		Map<Long, Set<String>> associations = graph.getTargetAssociations(oaID);
		List<Long> list = new ArrayList<Long>(associations.keySet());
		for (Long associate : list) {
			Set<String> accessRights = associations.get(associate);
			for (String accessRight : accessRights) {
				Graph mutant = createCopy();
				Set<String> copy = new HashSet<String>();
				copy.addAll(accessRights);
				copy.remove(accessRight);
				dissAndAssoc(mutant, associate, oaID, copy);
				testMutant(mutant, testSuite, testMethod, getNumberOfMutants(), mutationMethod);
				setNumberOfMutants(getNumberOfMutants() + 1);

			}
		}

	}

	private Graph dissAndAssoc(Graph mutant, Long associate, Long oaID, Set<String> accessRights) throws PMException {
		mutant.dissociate(associate, oaID);
		mutant.associate(associate, oaID, accessRights);
		return mutant;
	}
}
