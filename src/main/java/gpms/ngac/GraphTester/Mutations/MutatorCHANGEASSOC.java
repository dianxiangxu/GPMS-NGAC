package gpms.ngac.GraphTester.Mutations;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import gov.nist.csd.pm.exceptions.PMException;
import gov.nist.csd.pm.pip.graph.Graph;
import gov.nist.csd.pm.pip.graph.model.nodes.Node;

public class MutatorCHANGEASSOC extends MutantTester {
	String mutationMethod = "CHANGEASSOC";

	public void init(String testMethod) throws PMException, IOException {
		String testResults = "CSV/" + testMethod + "/" + testMethod + "testResultsCHANGEASSOC.csv";
		String testSuitePath = "CSV/testSuits/" + testMethod + "testSuite.csv";
		getGraphLoaded(initialGraphConfig);
		for (Node oa : OAs) {
			performMutation(oa, testMethod, testSuitePath);
		}
		saveCSV(data, new File(testResults), testMethod);

	}

	private void performMutation(Node oa, String testMethod, String testSuitePath) throws PMException, IOException {
		File testSuite = new File(testSuitePath);

		if (graph.getTargetAssociations(oa.getID()) == null) {
			return;
		}
		Map<Long, Set<String>> sources = graph.getTargetAssociations(oa.getID());
		List<Long> uas = new ArrayList<Long>(sources.keySet());

		for (Long pastSourceID : uas) {
			Set<String> accessRights = sources.get(pastSourceID);

			Graph mutant = createCopy();
			for (Node ua : UAs) {
				if (ua.getID() != pastSourceID) {
					changeAssociation(mutant, pastSourceID, oa.getID(), ua.getID(), accessRights);
					testMutant(mutant, testSuite, testMethod, getNumberOfMutants(), mutationMethod);
					setNumberOfMutants(getNumberOfMutants() + 1);
				}
			}
		}
	}

	private void changeAssociation(Graph mutant, Long pastSourceID, Long oaID, Long sourceToBeID,
			Set<String> accessRights) throws PMException, IOException {
		mutant.dissociate(pastSourceID, oaID);
		mutant.associate(sourceToBeID, oaID, accessRights);;
	}
}
