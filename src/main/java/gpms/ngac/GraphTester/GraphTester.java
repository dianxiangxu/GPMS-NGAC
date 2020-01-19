package gpms.ngac.GraphTester;

import gov.nist.csd.pm.exceptions.PMException;
import gov.nist.csd.pm.pip.graph.Graph;
import gov.nist.csd.pm.pip.graph.GraphSerializer;
import gov.nist.csd.pm.pip.graph.MemGraph;
import gov.nist.csd.pm.pip.graph.model.nodes.Node;
import gpms.ngac.policy.Constants;
import gpms.ngac.policy.NGACPolicyConfigurationLoader;
import gpms.ngac.GraphTester.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

public class GraphTester {

	public static void main(String[] args) throws Exception {
		NGACPolicyConfigurationLoader loader;
		loader = new NGACPolicyConfigurationLoader();
		// loader.init();

		// loader.setPolicy(gt.getGraph());
		// loader.savePolicy(gt.getGraph(),"C:/data/ngac_config_Vlad.json");

		GraphTester graphTester = new GraphTester();

		/*
		 * File file = new File("C:/Users/dubro/git/GPMS-NGAC/mutants/REMNODE"); for
		 * (final File fileEntry : file.listFiles()) {
		 * 
		 * String json = new
		 * String(Files.readAllBytes(Paths.get(fileEntry.getAbsolutePath())));
		 * 
		 * Graph graph = GraphSerializer.fromJson(new MemGraph(), json);
		 * System.out.println("Sneak path and Reachability:");
		 * GraphTesterReachabilitySneakpath gt = new
		 * GraphTesterReachabilitySneakpath(graph); List<Long> usersTrueFalse =
		 * gt.getUsers(); List<String[]> data = gt.testGraphPC();
		 * 
		 * graphTester.saveCSV(data); graphTester.COUNTER++; }
		 */
		GraphTesterReachabilitySneakpath gt1 = new GraphTesterReachabilitySneakpath();
		List<String[]> data1 = gt1.testGraphPC();
		graphTester.saveCSV(data1, "RS");

		GraphTesterReachability gt2 = new GraphTesterReachability();
		List<String[]> data2 = gt2.testGraphPC();
		graphTester.saveCSV(data2, "R");

		GraphTesterPairwiseNoPath gt3 = new GraphTesterPairwiseNoPath();
		List<String[]> data3 = gt3.testGraphPairwisePC();
		graphTester.saveCSV(data3, "PNP");
		GraphTesterPairwisePath gt4 = new GraphTesterPairwisePath();
		List<String[]> data4 = gt4.testGraphPairwisePC();
		graphTester.saveCSV(data4, "PP");

		// loader.setPolicy(gt1.getGraph());
		// loader.savePolicy(gt1.getGraph(),"C:/data/ngac_config_Vlad3.json");
		/*
		 * File folder = new File("C:/Users/dubro/git/GPMS-NGAC/CSV/RS"); int i = 0; for
		 * (final File fileEntry1 : folder.listFiles()) { if (i == 0) { i++; continue; }
		 * System.out.println("Killed number " +i + " mutant: " +
		 * !graphTester.compareCSV(new
		 * File("C:/Users/dubro/git/GPMS-NGAC/CSV/RS/csv0.csv"), fileEntry1)); i++; }
		 * System.out.println("Mutant death ratio:" + i/(folder.listFiles().length - 1)
		 * * 100 + "%"); System.out.println("Number of mutants" + (i-1));
		 */
		/*
		 * System.out.println("============================================");
		 * 
		 * //loader.setPolicy(gt.graph); //loader.savePolicy(null);
		 * System.out.println(""); System.out.println("Reachability:");
		 * 
		 * GraphTesterReachability gttrue = new GraphTesterReachability(); List<Long>
		 * usersTrue = gttrue.getUsers(); gttrue.testGraphPC();
		 * System.out.println("============================================");
		 * 
		 * System.out.println(""); System.out.println("Pairwise combinations Path:");
		 * GraphTesterPairwisePath gtptrue = new GraphTesterPairwisePath();
		 * gtptrue.testGraphPairwisePC();
		 * System.out.println("============================================");
		 * 
		 * System.out.println(""); System.out.println("Pairwise combinations Path:");
		 * GraphTesterPairwiseNoPath gtpfalse = new GraphTesterPairwiseNoPath();
		 * gtpfalse.testGraphPairwisePC();
		 * System.out.println("============================================");
		 * 
		 * 
		 * System.out.println("Mutation:");
		 * 
		 * //GraphTesterReachabilitySneakpathMutation gtRSMutation = new
		 * GraphTesterReachabilitySneakpathMutation(); //gtRSMutation.mutator();
		 * 
		 * 
		 * //loader.setPolicy(gttrue.graph); //loader.savePolicy(null); // Graph graph =
		 * loader.getPolicy(); //HashMap<String, String> map = new
		 * HashMap<String,String>();
		 * 
		 * // Set<Node> set = graph.search("ProposalCreation", "PC", map); // gh = new
		 * GraphTester(graph, set.toArray(new Node[set.size()])[0]); // boolean result =
		 * false; //result = gh.testValidUser("tenuredU"); //result =
		 * gh.testValidUser("researchU"); //result = gh.testValidUser("tenure-trackU");
		 * //result = gh.testValidUser("tenureTrackTenuredU"); //result =
		 * gh.testValidUser("PIElegibleU");
		 * 
		 * //System.out.println(result);
		 * 
		 */
	}

	private void saveCSV(List<String[]> data, String testMethod) throws PMException, IOException {
		boolean bool = true;
		String folderCSV = "CSV";
		File file = new File(folderCSV);
		if (!file.exists()) {
			bool = file.mkdir();
		}
		String folderTestSuits = "CSV/testSuits";
		File file2 = new File(folderTestSuits);
		if (!file2.exists() && bool) {
			bool = file2.mkdir();
		}
		if (bool) {
			System.out.println("The directory was created or was already there");
		} else {
			System.out.println("Failure with creating the directory");
			return;
		}
		String testSuiteFile = "CSV/testSuits/" + testMethod + "testSuite.csv";
		file = new File(testSuiteFile);
		if (file.createNewFile()) {
			System.out.println("File has been created.");
		} else {

			System.out.println("File already exists.");
		}
		BufferedWriter writer = null;
		writer = new BufferedWriter(new FileWriter(file));
		CSVWriter CSVwriter = new CSVWriter(writer);
		CSVwriter.writeAll(data);
		writer.flush();
		CSVwriter.close();

		if (writer != null)
			writer.close();

	}

}
