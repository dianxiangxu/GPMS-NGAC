package gpms.policy.GraphTester;

import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Set;

import gov.nist.csd.pm.exceptions.PMException;
import gov.nist.csd.pm.operations.OperationSet;
import gov.nist.csd.pm.pdp.decider.PReviewDecider;
import gov.nist.csd.pm.pip.graph.Graph;
import gov.nist.csd.pm.pip.graph.GraphSerializer;
import gov.nist.csd.pm.pip.graph.MemGraph;
import gov.nist.csd.pm.pip.graph.model.nodes.Node;
import gpms.policy.Constants;

public class TestSimpleGraph {
	public static void main(String[] args) throws Exception {
		TestSimpleGraph tsg = new TestSimpleGraph();
		tsg.buildAndTestSimpleGraph();
	}

	private void buildAndTestSimpleGraph() throws PMException, InterruptedException {

		// create graph
		Graph graph = new MemGraph();

		// create a policy class(String name, Map<String, String> properties)
		Node policy = graph.createPolicyClass("pc1", null);

		// create all nodes(String name, NodeType type, Map<String, String> properties,
		// String initialParent, String... additionalParents)
		Node ua2 = graph.createNode("ua2", UA, null, "pc1");
		Node ua1 = graph.createNode("ua1", UA, null, "ua2");
		Node u1 = graph.createNode("u1", UA, null, "ua1");
		Node u2 = graph.createNode("u2", UA, null, "ua2");
		Node oa2 = graph.createNode("oa2", UA, null, "pc1");
		Node oa1 = graph.createNode("oa1", UA, null, "oa2");
		Node o1 = graph.createNode("o1", UA, null, "oa1");
		Node o2 = graph.createNode("o2", UA, null, "oa2");

		// create associations
		graph.associate(ua1.getName(), oa1.getName(), new OperationSet("w"));
		graph.associate(ua2.getName(), oa2.getName(), new OperationSet("r", "x"));

		// create PReviewDecider object and provide it with a graph and prohibitions, prohibitions is null for this example
		// which in this case is null
		PReviewDecider decider = new PReviewDecider(graph, null);

		// get the list of the available access rights
		Set<String> list = decider.list(ua2.getName(), "", oa2.getName());

		// print them
		for (String s : list) {

			//System.out.println("UA2 AND OA2: " + s);
		}
		//System.out.println("------------------------------------------------------");

		for (String s : decider.list(ua1.getName(), "", oa2.getName())) {

			//System.out.println("UA1 AND OA2: " + s);
		}
		//System.out.println("------------------------------------------------------");

		for (String s : decider.list(ua2.getName(), "", oa1.getName())) {

			//System.out.println("UA2 AND OA1: " + s);
		}
		//System.out.println("------------------------------------------------------");

		for (String s : decider.list(ua1.getName(), "", oa1.getName())) {

			//System.out.println("UA1 AND OA1: " + s);
		}
		//System.out.println("------------------------------------------------------");

		// check if UA1 has {w} access right on OA2
		boolean result = decider.check(ua1.getName(), "", oa2.getName(), "w");
		//System.out.println("UA1 {w} OA1: " + result);// false

		// check if UA1 has {r} access right on OA2
		result = decider.check(ua1.getName(), "", oa2.getName(), "r");
		//System.out.println("UA1 {r} OA1: " + result);// true

		//convert graph to JSON String
		String jsonGraph = GraphSerializer.toJson(graph); // convert the graph to JSON

		try {
			//save the graph into the file on disk
			savePolicy(graph, System.getProperty("user.dir") + "\\src\\main\\resources\\test.json");
		} catch (IOException e) {
			e.printStackTrace();
		}

		//get the file that we just created from disk
		File testFile = getFileFromResources("test.json");
		String testJSON = "";
		try {
			//convert the file to String
			testJSON = new String(Files.readAllBytes(Paths.get(testFile.getAbsolutePath())));
		} catch (IOException e) {
			e.printStackTrace();
		}
		//create an empty graph
		Graph graphFromFile = new MemGraph();
		
		//put the string into the empty graph we created above
		GraphSerializer.fromJson(graphFromFile, testJSON);

		// create PReviewDecider object and provide it with a graph and prohibitions, prohibitions is null for this example
		PReviewDecider decider2 = new PReviewDecider(graphFromFile, null);
		// check if UA1 has {w} access right on OA2
		result = decider2.check(ua1.getName(), "", oa2.getName(), "w");
		//System.out.println("UA1 {w} OA1: " + result);// false

		// check if UA1 has {r} access right on OA2
		result = decider2.check(ua1.getName(), "", oa2.getName(), "r");
		//System.out.println("UA1 {r} OA1: " + result);// true

	}

	//is used to get a file from inside the project in resource folder 
	private File getFileFromResources(String fileName) {
		ClassLoader classLoader = this.getClass().getClassLoader();

		URL resource = classLoader.getResource(fileName);
		if (resource == null) {
			throw new IllegalArgumentException("file is not found!");
		} else {
			return new File(resource.getFile());
		}

	}
	//is used to save a file in any place on disk
	public static void savePolicy(Graph policy, String path) throws PMException, IOException {

		String policyString = GraphSerializer.toJson(policy);

		File file;
		if (path == null || path.isEmpty()) {
			//file = new File(Constants.POLICY_CONFIG_OUTPUT_FILE);
		} else {
			file = new File(path);
		}

//		if (file.createNewFile()) {
//			//System.out.println("File has been created.");
//		} else {
//
//			//System.out.println("File already exists.");
//		}
//
//		BufferedWriter writer = null;
//		writer = new BufferedWriter(new FileWriter(file));
//		writer.write(policyString);
//		writer.flush();
//
//		if (writer != null)
//			writer.close();

	}

}