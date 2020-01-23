package gpms.ngac.GraphTester.Mutations;

import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.OA;
import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.PC;
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
import java.util.HashMap;
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

abstract class MutantTester {
	private double numberOfKilledMutants = 0;
	private int numberOfMutants = 0;
	List<Set<String>> operations = new ArrayList<Set<String>>();

	List<String[]> data = new ArrayList<String[]>();
	List<Node> OAs;
	public static Graph graph;
	

	public String initialGraphConfig = "C:/data/ngac_config_Vlad3.json";
	List<Node> UAs;
	List<Node> UAsOAs;
	List<Node> UAsPCs;
	List<Node> UAsPCsOAs;
	public void testMutant(Graph mutant, File testSuiteCSV, String testMethod, int mutantNumber, String mutationMethod)
			throws PMException, IOException {

		List<String[]> testSuite = loadCSV(testSuiteCSV);
		if (mutantNumber == 0) {
			String[] header = new String[testSuite.size() + 2];
			header[0] = "MutantName";

			for (int i = 1; i < header.length; i++) {
				header[i] = "Test" + i;
			}
			header[header.length - 1] = "MutantKilled?";
			data.add(header);
		}

		PReviewDecider decider = new PReviewDecider(mutant);

		String[] mutantTest = new String[testSuite.size() + 2];
		mutantTest[0] = mutationMethod + (mutantNumber + 1);
		int counter = 1;

		for (String[] sArray : testSuite) {

			String UAname = sArray[2];
			String OAname = sArray[3];
			Long UA = getIDfromName(mutant, UAname, "UA");
			Long OA = getIDfromName(mutant, OAname, "OA");
			if (UA == null || OA == null) {
				mutantTest[counter] = "Fail";
				counter++;
				continue;
			}
			String[] AR = { sArray[4] };
			Boolean result = Boolean.parseBoolean(sArray[5]);
			if (decider.check(UA, 102L, OA, AR) != result) {
				mutantTest[counter] = "Fail";
			} else {
				mutantTest[counter] = "Pass";
			}
			counter++;
		}
		String mutantKilled = "";
		if (Arrays.stream(mutantTest).anyMatch("Fail"::equals)) {
			mutantKilled = "Yes";
			setNumberOfKilledMutants(getNumberOfKilledMutants() + 1);
		} else {
			mutantKilled = "No";
		}
		mutantTest[mutantTest.length - 1] = mutantKilled;
		data.add(mutantTest);

	}

	
	
	public Long getIDfromName(Graph graph, String name, String type) throws PMException {
		Set<Node> set = graph.search(name, type, null);
		if (set.size() != 0)
			return set.iterator().next().getID();
		else
			return null;
	}

	public double calculateMutationScore(double numberOfMutations, double numberOfKilledMutants) {
		return (numberOfKilledMutants / numberOfMutations * 100);
	}

	public List<String[]> loadCSV(File csv) throws IOException {

		Reader reader = Files.newBufferedReader(Paths.get(csv.getAbsolutePath()));
		CSVReader csvReader = new CSVReader(reader);
		List<String[]> csvList = csvReader.readAll();
		reader.close();
		csvReader.close();
		return csvList;
	}

	public void saveCSV(List<String[]> data, File directoryForTestResults, String testMethod) throws PMException, IOException {
		boolean bool = true;
		String folderCSV = "CSV";
		File file = new File(folderCSV);
		if (!file.exists()) {
			bool = file.mkdir();
		}
		String folderTests = "CSV/"+testMethod;
		File file2 = new File(folderTests);
		if (!file2.exists() && bool) {
			bool = file2.mkdir();
		}
		if (bool) {
			System.out.println("The directory was created or was already there");
		} else {
			System.out.println("Failure with creating the directory");
			return;
		}
		if (directoryForTestResults.createNewFile()) {
			System.out.println("File has been created.");
		} else {

			System.out.println("File already exists.");
		}
		BufferedWriter writer = null;
		writer = new BufferedWriter(new FileWriter(directoryForTestResults));
		CSVWriter CSVwriter = new CSVWriter(writer);
		CSVwriter.writeAll(data);
		writer.flush();
		CSVwriter.close();

		if (writer != null)
			writer.close();

	}

	public File getFileFromResources(String fileName) {
		File resource = new File(fileName);
		return resource;
	}

	public void getOAsInGraph() throws PMException {
		Node[] nodes = graph.getNodes().toArray(new Node[graph.getNodes().size()]);
		OAs = new ArrayList<Node>();
		for (Node node : nodes) {
			if (node.getType() == OA) {
				OAs.add(node);
			}

		}
	}

	public void getGraphLoaded(String filepath) throws PMException, IOException {
		File file = getFileFromResources(filepath);

		String json = new String(Files.readAllBytes(Paths.get(file.getAbsolutePath())));

		graph = GraphSerializer.fromJson(new MemGraph(), json);
		getOAsInGraph();
		if (OAs.size() == 0) {
			System.out.println("No OAs found");
			return;
		}
		getUAsInGraph();
		if (UAs.size() == 0) {
			System.out.println("No OAs found");
			return;
		}
		loadAssociations();
		
		getUAsPCsInGraph();
		if (UAsPCs.size() == 0) {
			System.out.println("No UAs and PCs found");
			return;
		}
		getUAsPCsOAsInGraph();
		if (UAsPCsOAs.size() == 0) {
			System.out.println("No UAs, PCs, and OAs found");
			return;
		}
		getUAsOAsInGraph();
	}

	private void getUAsInGraph() throws PMException {
		UAs = new ArrayList<Node>();

		Node[] nodes = graph.getNodes().toArray(new Node[graph.getNodes().size()]);
		for (Node node : nodes) {
			if (node.getType() == UA) {
				UAs.add(node);
			}
		}

	}
	private void getUAsOAsInGraph() throws PMException {
		UAsOAs = new ArrayList<Node>();

		Node[] nodes = graph.getNodes().toArray(new Node[graph.getNodes().size()]);
		for (Node node : nodes) {
			if (node.getType() == UA||node.getType() == OA) {
				UAsOAs.add(node);
			}
		}

	}
	private void getUAsPCsInGraph() throws PMException {
		UAsPCs = new ArrayList<Node>();

		Node[] nodes = graph.getNodes().toArray(new Node[graph.getNodes().size()]);
		for (Node node : nodes) {
			if (node.getType() == UA||node.getType() == PC) {
				UAsPCs.add(node);
			}
		}

	}
	private void getUAsPCsOAsInGraph() throws PMException {
		UAsPCsOAs = new ArrayList<Node>();

		Node[] nodes = graph.getNodes().toArray(new Node[graph.getNodes().size()]);
		for (Node node : nodes) {
			if (node.getType() == UA||node.getType() == PC|| node.getType() == OA) {
				UAsPCsOAs.add(node);
			}
		}

	}
	public double getNumberOfKilledMutants() {
		return numberOfKilledMutants;
	}

	public int getNumberOfMutants() {
		return numberOfMutants;
	}

	public void setNumberOfKilledMutants(double numberOfKilledMutants) {
		this.numberOfKilledMutants = numberOfKilledMutants;
	}
	public void setNumberOfMutants(int numberOfMutants) {
		this.numberOfMutants = numberOfMutants;
	}
	private void loadAssociations() throws PMException {
		Map<Long, Set<String>> associations = new HashMap<Long, Set<String>>();
		for (Node oa : OAs) {
			if (graph.getTargetAssociations(oa.getID()) != null)
			{
				associations.putAll(graph.getTargetAssociations(oa.getID()));
			}
			List<Long> list = new ArrayList<Long>(associations.keySet());
			
			for (int i = 0; i < list.size(); i++) {
				long objectID = list.get(i);
				Node obj = graph.getNode(objectID);
				
				if(!operations.contains(associations.get(objectID))){
					operations.add(associations.get(objectID));
				}
				//graph.dissociate(objectID, oa.getID());
			}
		}
	}
	public Graph createCopy() throws PMException {
		Graph mutant = new MemGraph();
		String json = GraphSerializer.toJson(graph);
		mutant = GraphSerializer.fromJson(new MemGraph(), json);
		return mutant;
	}
}
