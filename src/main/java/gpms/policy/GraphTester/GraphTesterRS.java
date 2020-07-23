package gpms.policy.GraphTester;

import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.OA;
import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.PC;
import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.UA;
import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.U;
import gov.nist.csd.pm.exceptions.PMException;
import gov.nist.csd.pm.operations.OperationSet;
import gov.nist.csd.pm.pip.graph.Graph;
import gov.nist.csd.pm.pip.graph.GraphSerializer;
import gov.nist.csd.pm.pip.graph.MemGraph;
import gov.nist.csd.pm.pip.graph.model.nodes.Node;
import gpms.policy.Constants;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import com.opencsv.CSVWriter;

public abstract class GraphTesterRS {
	Graph graph;
	List<String> users;
	Node policy;
	Node createPDS;
	Node writePDS;
	Node readPDS;
	List<Node> OAs = new ArrayList<Node>();
	List<Node> PCs = new ArrayList<Node>();

	public static Random rand = new Random();

	public static long getID() {
		return rand.nextLong();
	}
	public GraphTesterRS(Graph graph) throws PMException {
		this.graph = graph;
		Node policy = graph.getNode("ProposalCreation");

	
		Node[] nodes = graph.getNodes().toArray(
				new Node[graph.getNodes().size()]);
		for (Node node : nodes) {
			if(node.getType() == OA){
				OAs.add(node);
			}
			if(node.getType() == PC){
				PCs.add(node);
			}
		}

	}

	public GraphTesterRS() throws PMException, Exception {

		graph = buildGraphGPMS1();

	}

	private Graph buildGraphGPMS() throws PMException, InterruptedException {
		graph = new MemGraph();
		policy = graph.createPolicyClass("ProposalCreation", null);
		PCs.add(policy);

		OperationSet operationsPI = new OperationSet();
		operationsPI.add("create-oa");
		operationsPI.add("create-oa-to-oa");
		
		OperationSet operationsNon = new OperationSet();
		operationsNon.add("write");
		operationsNon.add("read");
		createPDS = graph.createNode("createPDS", OA, null,"ProposalCreation");
		writePDS = graph.createNode("writePDS", OA, null, "ProposalCreation");
		readPDS = graph.createNode("readPDS", OA, null, "ProposalCreation");
		OAs.add(createPDS);
		OAs.add(writePDS);
		OAs.add(readPDS);
		Node facultyUA = graph.createNode("faculty", UA, null,"ProposalCreation");

		Node nonTenureTrackUA = graph.createNode("non-tenure-track",
				UA, null,"faculty");
		Node adjunctUA = graph.createNode("adjunct", UA, null,"faculty" );
		Node clinicalUA = graph.createNode( "clinical", UA, null,"non-tenure-track");
		Node teachingUA = graph.createNode("teaching", UA, null, "non-tenure-track");
		Node researchUA = graph.createNode("research", UA, null, "non-tenure-track");
		Node tenureTrackTenuredUA = graph.createNode(
				"tenureTrackTenured", UA, null, "faculty");
		Node tenureTrackUA = graph
				.createNode( "tenure-track", UA, null,"tenureTrackTenured");
		Node tenuredUA = graph.createNode("tenured", UA, null, "tenureTrackTenured");



		Node SPElegibleUA = graph.createNode( "SPElegible", UA, null, "ProposalCreation");
		graph.assign(nonTenureTrackUA.getName(), SPElegibleUA.getName());
		graph.assign(tenureTrackTenuredUA.getName(), SPElegibleUA.getName());

		Node CoPIElegibleUA = graph.createNode( "CoPIElegible", UA,
				null, "ProposalCreation");
		graph.assign(nonTenureTrackUA.getName(), CoPIElegibleUA.getName());
		graph.assign(tenureTrackTenuredUA.getName(), CoPIElegibleUA.getName());

		Node PIElegibleUA = graph.createNode("PIElegible", UA, null,"ProposalCreation");
		graph.assign(researchUA.getName(), PIElegibleUA.getName());
		graph.assign(tenureTrackTenuredUA.getName(), PIElegibleUA.getName());


		graph.assign(PIElegibleUA.getName(), policy.getName());

		graph.associate(PIElegibleUA.getName(), createPDS.getName(), operationsPI);
		
		graph.associate(facultyUA.getName(), writePDS.getName(), operationsNon);
	
		
		graph.associate(tenuredUA.getName(), readPDS.getName(), operationsNon);
	

		users = new ArrayList<String>();
		users.add(tenuredUA.getName());
		users.add(tenureTrackUA.getName());
		users.add(researchUA.getName());
		users.add(teachingUA.getName());
		users.add(clinicalUA.getName());
		users.add(adjunctUA.getName());
		users.add(PIElegibleUA.getName());

				
		return graph;
	}
	private Graph buildGraphGPMS1() throws PMException, InterruptedException {
		graph = new MemGraph();
		policy = graph.createPolicyClass("ProposalCreation", null);
		PCs.add(policy);

		OperationSet operationsPI = new OperationSet();
		operationsPI.add("create-oa");
		operationsPI.add("create-oa-to-oa");
		
		OperationSet operationsNon = new OperationSet();
		operationsNon.add("write");
		operationsNon.add("read");
		createPDS = graph.createNode("createPDS", OA, null,"ProposalCreation");
		writePDS = graph.createNode("writePDS", OA, null, "ProposalCreation");
		readPDS = graph.createNode("readPDS", OA, null, "ProposalCreation");
		OAs.add(createPDS);
		OAs.add(writePDS);
		OAs.add(readPDS);
		Node facultyUA = graph.createNode("faculty", UA, null,"ProposalCreation");

		Node nonTenureTrackUA = graph.createNode("non-tenure-track",
				UA, null,"faculty");
		Node adjunctUA = graph.createNode("adjunct", UA, null,"faculty" );
		Node clinicalUA = graph.createNode( "clinical", UA, null,"non-tenure-track");
		Node teachingUA = graph.createNode("teaching", UA, null, "non-tenure-track");
		Node researchUA = graph.createNode("research", UA, null, "non-tenure-track");
		Node tenureTrackTenuredUA = graph.createNode(
				"tenureTrackTenured", UA, null, "faculty");
		Node tenureTrackUA = graph
				.createNode( "tenure-track", UA, null,"tenureTrackTenured");
		Node tenuredUA = graph.createNode("tenured", UA, null, "tenureTrackTenured");



		Node SPElegibleUA = graph.createNode( "SPElegible", UA, null, "ProposalCreation");
		graph.assign(nonTenureTrackUA.getName(), SPElegibleUA.getName());
		graph.assign(tenureTrackTenuredUA.getName(), SPElegibleUA.getName());

		Node CoPIElegibleUA = graph.createNode( "CoPIElegible", UA,
				null, "ProposalCreation");
		graph.assign(nonTenureTrackUA.getName(), CoPIElegibleUA.getName());
		graph.assign(tenureTrackTenuredUA.getName(), CoPIElegibleUA.getName());

		Node PIElegibleUA = graph.createNode("PIElegible", UA, null,"ProposalCreation");
		graph.assign(researchUA.getName(), PIElegibleUA.getName());
		graph.assign(tenureTrackTenuredUA.getName(), PIElegibleUA.getName());


		graph.assign(PIElegibleUA.getName(), policy.getName());

		graph.associate(PIElegibleUA.getName(), createPDS.getName(), operationsPI);
		
		graph.associate(facultyUA.getName(), writePDS.getName(), operationsNon);
	
		
		graph.associate(tenuredUA.getName(), readPDS.getName(), operationsNon);
	

		users = new ArrayList<String>();
		users.add(tenuredUA.getName());
		users.add(tenureTrackUA.getName());
		users.add(researchUA.getName());
		users.add(teachingUA.getName());
		users.add(clinicalUA.getName());
		users.add(adjunctUA.getName());
		users.add(PIElegibleUA.getName());


		return graph;
	}
	public Graph getGraph(){
		return graph;
	}
	public List<String> getUsers(){
		return users;
	}
	public List<String[]> testGraphPC() throws PMException, IOException {
		return null;
		
	}
	public void testGraphPC(List<Long> list) throws PMException {
		
	}
	// Java program to print all combination of size r in an array of size n 
	  

		  
		    /* arr[]  ---> Input Array 
		    data[] ---> Temporary array to store current combination 
		    start & end ---> Staring and Ending indexes in arr[] 
		    index  ---> Current index in data[] 
		    r ---> Size of a combination to be printed */
		    static void combinationUtil(int arr[], int n, int r, int index, 
		                                int data[], int i) 
		    { 
		        // Current combination is ready to be printed, print it 
		        if (index == r) 
		        { 
		            for (int j=0; j<r; j++) 
		                System.out.print(data[j]+" "); 
		            //System.out.println(""); 
		        return; 
		        } 
		  
		        // When no more elements are there to put in data[] 
		        if (i >= n) 
		        return; 
		  
		        // current is included, put next at next location 
		        data[index] = arr[i]; 
		        combinationUtil(arr, n, r, index+1, data, i+1); 
		  
		        // current is excluded, replace it with next (Note that 
		        // i+1 is passed, but index is not changed) 
		        combinationUtil(arr, n, r, index, data, i+1); 
		    } 
		  
		    // The main function that prints all combinations of size r 
		    // in arr[] of size n. This function mainly uses combinationUtil() 
		   /* Method 2 (Include and Exclude every element)
		    Like the above method, We create a temporary array data[]. The idea here is similar to Subset Sum Problem. We one by one consider every element of input array, and recur for two cases:

		    1) The element is included in current combination (We put the element in data[] and increment next available index in data[])
		    2) The element is excluded in current combination (We do not put the element and do not change index)

		    When number of elements in data[] become equal to r (size of a combination), we print it.

		    This method is mainly based on Pascal’s Identity, i.e. ncr = n-1cr + n-1cr-1

		    Following is implementation of method 2.
		    https://www.geeksforgeeks.org/print-all-possible-combinations-of-r-elements-in-a-given-array-of-size-n/*/
		    static void printCombination(int arr[], int n, int r) 
		    { 
		        // A temporary array to store all combination one by one 
		        int data[]=new int[r]; 
		  
		        // Print all combination using temprary array 'data[]'
		        
		        for(int i=0; i<r+1; i++){
		        combinationUtil(arr, n, i, 0, data, 0); 
		        }
		    
		    }
		    
		    private File getFileFromResources(String fileName) {
				ClassLoader classLoader = this.getClass().getClassLoader();

				URL resource = classLoader.getResource(fileName);
				if (resource == null) {
					throw new IllegalArgumentException("file is not found!");
				} else {
					return new File(resource.getFile());
				}

			}
		} 


