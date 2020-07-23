package gpms.policy.GraphTester.Mutations;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import gov.nist.csd.pm.exceptions.PMException;
import gov.nist.csd.pm.operations.OperationSet;
import gov.nist.csd.pm.pip.graph.Graph;
import gov.nist.csd.pm.pip.graph.model.nodes.Node;

public class MutatorCHANGEASSOC extends MutantTester {
	String mutationMethod = "CHANGEASSOC";

	public void init(String testMethod) throws PMException, IOException {
		String testResults = "CSV/" + testMethod + "/" + testMethod + "testResultsCHANGEASSOC.csv";
		String testSuitePath = "CSV/testSuits/" + testMethod + "testSuite.csv";
		getGraphLoaded(initialGraphConfig);
		for (Node oaua : UAsOAs) {
			performMutation(oaua, testMethod, testSuitePath);
		}
		saveCSV(data, new File(testResults), testMethod);

	}

	private void performMutation(Node oa, String testMethod, String testSuitePath) throws PMException, IOException {
		File testSuite = new File(testSuitePath);

		if (graph.getTargetAssociations(oa.getName()) == null) {
			return;
		}
		Map<String, OperationSet> sources = graph.getTargetAssociations(oa.getName());
		List<String> uas = new ArrayList<String>(sources.keySet());

		for (String pastSourceID : uas) {
			Set<String> accessRights1 = sources.get(pastSourceID);
			OperationSet accessRights = new OperationSet(accessRights1);
			
			Graph mutant = createCopy();
			for (Node ua : UAs) {
				if (ua.getName().equals(pastSourceID)) {
					changeAssociation(mutant, pastSourceID, oa.getName(), ua.getName(), accessRights);
					testMutant(mutant, testSuite, testMethod, getNumberOfMutants(), mutationMethod);
					setNumberOfMutants(getNumberOfMutants() + 1);
				}
			}
		}
	}

	private void changeAssociation(Graph mutant, String pastSourceID, String oaID, String sourceToBeID,
			OperationSet accessRights) throws PMException, IOException {
		mutant.dissociate(pastSourceID, oaID);
		mutant.associate(sourceToBeID, oaID, accessRights);;
	}
}
