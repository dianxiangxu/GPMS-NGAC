package gpms.ngac.GraphTester.Mutations;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

import gov.nist.csd.pm.exceptions.PMException;
import gov.nist.csd.pm.operations.OperationSet;
import gov.nist.csd.pm.pdp.decider.PReviewDecider;
import gov.nist.csd.pm.pip.graph.Graph;
import gov.nist.csd.pm.pip.graph.GraphSerializer;
import gov.nist.csd.pm.pip.graph.MemGraph;
import gov.nist.csd.pm.pip.graph.model.nodes.Node;
import gpms.ngac.policy.Constants;
import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.OA;
import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.PC;
import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.UA;

public class MutatorDISS extends MutantTester{
	String mutationMethod = "DISS";

	public void init(String testMethod) throws PMException, IOException {
		String testResults = "CSV/"+testMethod+"/"+testMethod+"testResultsDISS.csv";
		String testSuitePath = "CSV/testSuits/"+testMethod+"testSuite.csv";
		getGraphLoaded(initialGraphConfig);
		
		for(Node oa : OAs) {
			performMutation(oa,testMethod,testSuitePath);
		}
		saveCSV(data, new File(testResults), testMethod);

	}

	

	private void performMutation(Node oa, String testMethod, String testSuitePath) throws PMException, IOException {
		File testSuite = new File(testSuitePath);

			Graph mutant = createCopy();			
			if (graph.getTargetAssociations(oa.getName()) != null) {
				Map<String, OperationSet> associations = graph.getTargetAssociations(oa.getName());
				List<String> list = new ArrayList<String>(associations.keySet());
				for(String associate : list) {
					mutant.dissociate(associate, oa.getName());					
					testMutant(mutant, testSuite,testMethod , getNumberOfMutants(), mutationMethod);
					setNumberOfMutants(getNumberOfMutants() + 1);
				}
			}
		}		
	
	}

	

	
	
	
	
		
	
	

	

	


	

