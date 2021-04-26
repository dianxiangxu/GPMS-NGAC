package gpms.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import org.apache.commons.io.FileUtils;

import com.github.javafaker.Faker;


import gov.nist.csd.pm.exceptions.PMException;
import gov.nist.csd.pm.pip.graph.Graph;
import gov.nist.csd.pm.pip.graph.GraphSerializer;
import gov.nist.csd.pm.pip.graph.MemGraph;
import gov.nist.csd.pm.pip.graph.model.nodes.Node;

import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.UA;
import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.U;

public class GPMSGraphGeneratorRealistic {

	//final int numberOfColleges = 10;
	//final int numberOfDepartments = 2;
	final int numberOfUsers = 2;

	List<String> existentCollegeList = new ArrayList<String>();

	List<String> collegeLettersList = new ArrayList<String>();
	List<String> collegeDepartmentList = new ArrayList<String>();;
	List<String> userNames = new ArrayList<String>();;

	Graph academicGeneratedGraph = new MemGraph();
	Graph academicUnitsPolicyClassGraph = new MemGraph();

	Graph eligibilityGeneratedGraph = new MemGraph();
	Graph eligibilityPolicyClassGraph = new MemGraph();
	HashMap<String, ArrayList<String>> colleges = new HashMap<String, ArrayList<String>>();

	HashMap<String, ArrayList<String>> capitalCities = new HashMap<String, ArrayList<String>>();

	String folderName = "src/main/resources/GPMS_Generated";

	public static void main(String[] args) throws Exception {
		final long startTime = System.currentTimeMillis();

		GPMSGraphGeneratorRealistic gg = new GPMSGraphGeneratorRealistic();

		String AcademicUnitsPolicyClass = "src/main/resources/GPMS/AcademicUnitsPolicyClass.json";
		String EligibilityPolicyClass = "src/main/resources/GPMS/EligibilityPolicyClass.json";

		gg.academicUnitsPolicyClassGraph = readAnyGraph(AcademicUnitsPolicyClass);

		gg.eligibilityPolicyClassGraph = readAnyGraph(EligibilityPolicyClass);
		GraphSerializer.fromJson(gg.eligibilityGeneratedGraph, GraphSerializer.toJson(gg.eligibilityPolicyClassGraph));

		// System.out.println(initialGraph.getChildren("AcademicUnitsPolicyClass"));
		gg.getExistentCollegeLetters(gg.academicUnitsPolicyClassGraph.getChildren("AcademicUnitsPolicyClass"));
		gg.createNewColleges();

		gg.generateNewGraph(gg.academicUnitsPolicyClassGraph);

		gg.checkOrCreateFolder();

		saveDataToFile(GraphSerializer.toJson(gg.academicGeneratedGraph),
				gg.folderName + "/AcademicUnitsPolicyClass.json");
		saveDataToFile(GraphSerializer.toJson(gg.eligibilityGeneratedGraph),
				gg.folderName + "/EligibilityPolicyClass.json");
		final long endTime = System.currentTimeMillis();

		System.out.println("Total execution time: " + (endTime - startTime));

		gg.copyDefaultPolicies();
		saveDataToFile(gg.getCompleteInfo(), gg.folderName + "/info.txt");

	}

	private void copyDefaultPolicies() throws Exception {
		File originalEditing = new File("src/main/resources/GPMS/EditingPolicyClass.json");
		File originalAdministration = new File("src/main/resources/GPMS/AdministrationUnitsPolicyClass.json");
		File copyEditing = new File(folderName + "/EditingPolicyClass.json");
		File copyAdministration = new File(folderName + "/AdministrationUnitsPolicyClass.json");
		FileUtils.copyFile(originalEditing, copyEditing);
		FileUtils.copyFile(originalAdministration, copyAdministration);
	}

	private String getCompleteInfo() throws Exception {
		StringBuilder sb = new StringBuilder();
		sb.append("Eligibility Policy Class:");
		sb.append(System.lineSeparator());
		sb.append(getGraphInfo(eligibilityGeneratedGraph));
		sb.append("Academic Policy Class:");
		sb.append(System.lineSeparator());
		sb.append(getGraphInfo(academicGeneratedGraph));

		// default policies
		Graph editingGraph = readAnyGraph("src/main/resources/GPMS/EditingPolicyClass.json");
		Graph administrationGraph = readAnyGraph("src/main/resources/GPMS/AdministrationUnitsPolicyClass.json");

		sb.append("Editing Policy Class:");
		sb.append(System.lineSeparator());
		sb.append(getGraphInfo(editingGraph));
		sb.append("Administration Policy Class:");
		sb.append(System.lineSeparator());
		sb.append(getGraphInfo(administrationGraph));
		int totalSize = eligibilityGeneratedGraph.getNodes().size() + academicGeneratedGraph.getNodes().size()
				+ editingGraph.getNodes().size() + administrationGraph.getNodes().size();
		sb.append("TOTAL NODES: " + totalSize);
		return sb.toString();
	}

	private String getGraphInfo(Graph graph) throws Exception {
		StringBuilder sb = new StringBuilder();

		sb.append("     UA : " + Integer.toString(getNodesByTypes(graph, "UA").size()));
		sb.append(System.lineSeparator());
		sb.append("     U: " + Integer.toString(getNodesByTypes(graph, "U").size()));
		sb.append(System.lineSeparator());
		sb.append("     OA: " + Integer.toString(getNodesByTypes(graph, "OA").size()));
		sb.append(System.lineSeparator());
		sb.append("     O: " + Integer.toString(getNodesByTypes(graph, "O").size()));
		sb.append(System.lineSeparator());
		sb.append("     PC: " + Integer.toString(getNodesByTypes(graph, "PC").size()));
		sb.append(System.lineSeparator());

		return sb.toString();
	}

	private void checkOrCreateFolder() {
		File theDir = new File(folderName);
		if (!theDir.exists()) {
			theDir.mkdirs();
		}
	}

	private void getExistentCollegeLetters(Set<String> currentColleges) {
		for (String s : currentColleges) {
			String[] splitted = s.split(" ");
			existentCollegeList.add(splitted[1]);
		}
	}

	private void createNewColleges() throws PMException {
		collegeLettersList.add("CHS");
		collegeLettersList.add("EDU");
		collegeLettersList.add("MNGT");
		collegeLettersList.add("LAW");
		ArrayList<String> COE = new ArrayList<String>();
		COE.add("MEC");
		COE.add("INFO");
		ArrayList<String> COAS = new ArrayList<String>();
		COAS.add("ECON");
		COAS.add("MATH");
		COAS.add("BIO");
		ArrayList<String> CHS = new ArrayList<String>();
		CHS.add("DENT");
		CHS.add("MED");
		CHS.add("NURS");
		CHS.add("PHA");
		CHS.add("HSE");
		ArrayList<String> EDU = new ArrayList<String>();
		EDU.add("EARLY");
		EDU.add("ELEM");
		EDU.add("HSL");
		EDU.add("MSL");
		EDU.add("FORL");
		ArrayList<String> MNGT = new ArrayList<String>();
		MNGT.add("BA");
		MNGT.add("ACC");
		MNGT.add("FIN");
		MNGT.add("ENTREP");
		MNGT.add("URB");
		ArrayList<String> LAW = new ArrayList<String>();
		LAW.add("LLM");
		LAW.add("JD");
		LAW.add("LLMT");
		LAW.add("FAM");
		LAW.add("INTL");
		colleges.put("COE", COE);
		colleges.put("COAS", COAS);
		colleges.put("CHS", CHS);
		colleges.put("EDU", EDU);
		colleges.put("MNGT", MNGT);
		colleges.put("LAW", LAW);

	}

	private void generateNewGraph(Graph initialGraph) throws PMException {
		GraphSerializer.fromJson(academicGeneratedGraph, GraphSerializer.toJson(initialGraph));
		generateAllNodes();
	}

	private void generateAllNodes() throws PMException {

		Set<Entry<String, ArrayList<String>>> EntrySet = colleges.entrySet();
		for (Map.Entry<String, ArrayList<String>> entry : EntrySet) {
			System.out.println(entry.getKey() + "/" + entry.getValue());
			String college = entry.getKey();
			if (!academicGeneratedGraph.exists("Dean " + college)) {
				academicGeneratedGraph.createNode("Dean " + college, UA, null, "AcademicUnitsPolicyClass");
			}
			ArrayList<String> departments = entry.getValue();
			for (String department : departments) {
				academicGeneratedGraph.createNode("BM" + department, UA, null, "Dean " + college);
				academicGeneratedGraph.createNode("bm" + department + "User", U, null, "BM" + department);
				academicGeneratedGraph.createNode("Chair" + department, UA, null, "BM" + department);
				academicGeneratedGraph.createNode("Chair" + department + "User", U, null, "Chair" + department);
				academicGeneratedGraph.createNode("Dept" + department, UA, null, "Chair" + department);
				// GENERATE NAMES HERE
				generateNames();
				for (String name : userNames) {
					academicGeneratedGraph.createNode(name, U, null, "Dept" + department);
				}
				assignNewUsersToRandomElegibility();
			}
		}
	}

	private void generateNames() throws PMException {
		userNames = new ArrayList<String>();
		while (userNames.size() < numberOfUsers) {
			Faker faker = new Faker();
			String firstName = faker.name().firstName();
			if (academicGeneratedGraph.exists(firstName) || eligibilityGeneratedGraph.exists(firstName)
					|| userNames.contains(firstName)) {
				continue;
			}
			userNames.add(firstName);
		}
		// String name = faker.name().fullName();
		// String firstName = faker.name().firstName();
		// String lastName = faker.name().lastName();
		// String streetAddress = faker.address().streetAddress();
	}

	private void assignNewUsersToRandomElegibility() throws PMException {
		String[] facultyTypes = { "TenureTrack Faculty", "Tenured Faculty", "Adjunct Faculty", "Research Faculty",
				"Clinic Faculty", "Teaching Faculty" };
		Random rand = new Random();
		for (String name : userNames) {
			int random_index = rand.nextInt(facultyTypes.length);
			eligibilityGeneratedGraph.createNode(name, U, null, facultyTypes[random_index]);
		}
	}
	
	public static Graph readAnyGraph(String path) throws PMException, IOException {
		File graphFile = new File(path);
		String graphJSON = new String(Files.readAllBytes(Paths.get(graphFile.getAbsolutePath())));
		Graph ngacGraph = new MemGraph();
		GraphSerializer.fromJson(ngacGraph, graphJSON);
		return ngacGraph;
	}
	public static List<String> getNodesByTypes(Graph graph, String... types)
			throws Exception {
		if (types.length == 0) {
			throw new Exception("Please provide at least one type to search for in graph");
		}
		List<String> listOfTypes = Arrays.asList(types);
		List<String> nodesToReturn = new ArrayList<String>();
		Node[] nodes = graph.getNodes().toArray(new Node[graph.getNodes().size()]);
		for (Node node : nodes) {
			if (listOfTypes.contains(node.getType().toString())) {
				nodesToReturn.add(node.getName());
			}
		}
		return nodesToReturn;
	}
	
	public static void saveDataToFile(String data, String path) throws PMException, IOException {
		File file = new File(path);
		FileWriter myWriter = new FileWriter(file);
		myWriter.write(data);
		myWriter.close();

	}
}
