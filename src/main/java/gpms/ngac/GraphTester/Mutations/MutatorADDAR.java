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
import gov.nist.csd.pm.pip.prohibitions.MemProhibitions;
import gov.nist.csd.pm.pip.prohibitions.Prohibitions;
import gov.nist.csd.pm.pip.prohibitions.ProhibitionsSerializer;
import gov.nist.csd.pm.pip.prohibitions.model.Prohibition;
import gov.nist.csd.pm.pip.prohibitions.model.Prohibition.Subject;
import gpms.ngac.policy.NGACPolicyConfigurationLoader;

public class MutatorADDAR extends MutantTester {
	String mutationMethod = "ADDAR";

	public void init(String testMethod) throws PMException, IOException {
		String testResults = "CSV/"+testMethod+"/"+testMethod+"testResultsADDAR.csv";
		String testSuitePath = "CSV/testSuits/"+testMethod+"testSuite.csv";
		getGraphLoaded(initialGraphConfig);
		for (Node oa : OAs) {

			Long oaID = oa.getID();
			if (graph.getTargetAssociations(oa.getID()) != null) {
				performMutation(oaID, testMethod, testSuitePath);
			}

		}
		saveCSV(data, new File(testResults), testMethod);
		

	}

	private void performMutation(Long oaID, String testMethod, String testSuitePath) throws PMException, IOException {
		File testSuite = new File(testSuitePath);

		Map<Long, Set<String>> associations = graph.getTargetAssociations(oaID);
		List<Long> targetAssociations = new ArrayList<Long>(associations.keySet());
		for (Long associate : targetAssociations) {
			Set<String> accessRights = associations.get(associate);
			accessRights.add("NewAR");
			Graph mutant = createCopy();
			mutant = dissAndAssoc(mutant, associate, oaID, accessRights);
			testMutant(mutant, testSuite,testMethod , getNumberOfMutants(), mutationMethod);
			setNumberOfMutants(getNumberOfMutants() + 1);
		}
	}

	

	private Graph dissAndAssoc(Graph mutant, Long associate, Long oaID, Set<String> accessRights) throws PMException {
		mutant.dissociate(associate, oaID);
		mutant.associate(associate, oaID, accessRights);
		return mutant;
	}



	
}
