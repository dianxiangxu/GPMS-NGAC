package gpms.ngac.GraphTester;

import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.OA;
import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.PC;
import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.UA;
import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.U;
import gov.nist.csd.pm.exceptions.PMException;
import gov.nist.csd.pm.pip.graph.Graph;
import gov.nist.csd.pm.pip.graph.GraphSerializer;
import gov.nist.csd.pm.pip.graph.MemGraph;
import gov.nist.csd.pm.pip.graph.model.nodes.Node;
import gpms.ngac.policy.Constants;

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
	List<Long> users;
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
		Set<Node> set1 = graph.search("ProposalCreation", "PC", new HashMap<String, String>());
		Node[] array1 = set1.toArray(new Node[graph.getNodes().size()]);
		policy = array1[0];

	
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
		
		policy = graph.createNode(getID(), "ProposalCreation", PC, null);
		PCs.add(policy);
		Set<String> operations = new HashSet<String>();
		operations.add("create-oa");
		operations.add("create-oa-to-oa");
		createPDS = graph.createNode(getID(), "org_PDSs", OA, null);
		graph.assign(createPDS.getID(), policy.getID());

		Node adjunctUA = graph.createNode(getID(), "adjunct", UA, null);
		Node clinicalUA = graph.createNode(getID(), "clinical", UA, null);
		Node teachingUA = graph.createNode(getID(), "teaching", UA, null);
		Node researchUA = graph.createNode(getID(), "research", UA, null);
		Node tenureTrackUA = graph
				.createNode(getID(), "tenure-track", UA, null);
		Node tenuredUA = graph.createNode(getID(), "tenured", UA, null);

		Node nonTenureTrackUA = graph.createNode(getID(), "non-tenure-track",
				UA, null);
		graph.assign(clinicalUA.getID(), nonTenureTrackUA.getID());
		graph.assign(teachingUA.getID(), nonTenureTrackUA.getID());
		graph.assign(researchUA.getID(), nonTenureTrackUA.getID());

		Node tenureTrackTenuredUA = graph.createNode(getID(),
				"tenureTrackTenured", UA, null);
		graph.assign(tenureTrackUA.getID(), tenureTrackTenuredUA.getID());
		graph.assign(tenuredUA.getID(), tenureTrackTenuredUA.getID());

		Node facultyUA = graph.createNode(getID(), "faculty", UA, null);
		graph.assign(adjunctUA.getID(), facultyUA.getID());
		graph.assign(nonTenureTrackUA.getID(), facultyUA.getID());
		graph.assign(tenureTrackTenuredUA.getID(), facultyUA.getID());

		Node SPElegibleUA = graph.createNode(getID(), "SPElegible", UA, null);
		graph.assign(nonTenureTrackUA.getID(), SPElegibleUA.getID());
		graph.assign(tenureTrackTenuredUA.getID(), SPElegibleUA.getID());

		Node CoPIElegibleUA = graph.createNode(getID(), "CoPIElegible", UA,
				null);
		graph.assign(nonTenureTrackUA.getID(), CoPIElegibleUA.getID());
		graph.assign(tenureTrackTenuredUA.getID(), CoPIElegibleUA.getID());

		Node PIElegibleUA = graph.createNode(getID(), "PIElegible", UA, null);
		graph.assign(researchUA.getID(), PIElegibleUA.getID());
		graph.assign(tenureTrackTenuredUA.getID(), PIElegibleUA.getID());

		graph.assign(facultyUA.getID(), policy.getID());
		graph.assign(SPElegibleUA.getID(), policy.getID());
		graph.assign(CoPIElegibleUA.getID(), policy.getID());
		graph.assign(PIElegibleUA.getID(), policy.getID());

		graph.associate(PIElegibleUA.getID(), createPDS.getID(), operations);
		graph.associate(clinicalUA.getID(), createPDS.getID(), operations);

		users = new ArrayList<Long>();
		users.add(tenuredUA.getID());
		users.add(tenureTrackUA.getID());
		users.add(researchUA.getID());
		users.add(teachingUA.getID());
		users.add(clinicalUA.getID());
		users.add(adjunctUA.getID());
				
		return graph;
	}
	private Graph buildGraphGPMS1() throws PMException, InterruptedException {
		graph = new MemGraph();
		policy = graph.createNode(getID(), "ProposalCreation", PC, null);
		PCs.add(policy);

		Set<String> operationsPI = new HashSet<String>();
		operationsPI.add("create-oa");
		operationsPI.add("create-oa-to-oa");
		
		Set<String> operationsNon = new HashSet<String>();
		operationsNon.add("write");
		operationsNon.add("read");
		createPDS = graph.createNode(getID(), "createPDS", OA, null);
		writePDS = graph.createNode(getID(), "writePDS", OA, null);
		readPDS = graph.createNode(getID(), "readPDS", OA, null);
		OAs.add(createPDS);
		OAs.add(writePDS);
		OAs.add(readPDS);
		
		graph.assign(createPDS.getID(), policy.getID());
		graph.assign(writePDS.getID(), policy.getID());
		graph.assign(readPDS.getID(), policy.getID());


		Node adjunctUA = graph.createNode(getID(), "adjunct", UA, null);
		Node clinicalUA = graph.createNode(getID(), "clinical", UA, null);
		Node teachingUA = graph.createNode(getID(), "teaching", UA, null);
		Node researchUA = graph.createNode(getID(), "research", UA, null);
		Node tenureTrackUA = graph
				.createNode(getID(), "tenure-track", UA, null);
		Node tenuredUA = graph.createNode(getID(), "tenured", UA, null);

		Node nonTenureTrackUA = graph.createNode(getID(), "non-tenure-track",
				UA, null);
		graph.assign(clinicalUA.getID(), nonTenureTrackUA.getID());
		graph.assign(teachingUA.getID(), nonTenureTrackUA.getID());
		graph.assign(researchUA.getID(), nonTenureTrackUA.getID());

		Node tenureTrackTenuredUA = graph.createNode(getID(),
				"tenureTrackTenured", UA, null);
		graph.assign(tenureTrackUA.getID(), tenureTrackTenuredUA.getID());
		graph.assign(tenuredUA.getID(), tenureTrackTenuredUA.getID());

		Node facultyUA = graph.createNode(getID(), "faculty", UA, null);
		graph.assign(adjunctUA.getID(), facultyUA.getID());
		graph.assign(nonTenureTrackUA.getID(), facultyUA.getID());
		graph.assign(tenureTrackTenuredUA.getID(), facultyUA.getID());

		Node SPElegibleUA = graph.createNode(getID(), "SPElegible", UA, null);
		graph.assign(nonTenureTrackUA.getID(), SPElegibleUA.getID());
		graph.assign(tenureTrackTenuredUA.getID(), SPElegibleUA.getID());

		Node CoPIElegibleUA = graph.createNode(getID(), "CoPIElegible", UA,
				null);
		graph.assign(nonTenureTrackUA.getID(), CoPIElegibleUA.getID());
		graph.assign(tenureTrackTenuredUA.getID(), CoPIElegibleUA.getID());

		Node PIElegibleUA = graph.createNode(getID(), "PIElegible", UA, null);
		graph.assign(researchUA.getID(), PIElegibleUA.getID());
		graph.assign(tenureTrackTenuredUA.getID(), PIElegibleUA.getID());

		graph.assign(facultyUA.getID(), policy.getID());
		graph.assign(SPElegibleUA.getID(), policy.getID());
		graph.assign(CoPIElegibleUA.getID(), policy.getID());
		graph.assign(PIElegibleUA.getID(), policy.getID());

		graph.associate(PIElegibleUA.getID(), createPDS.getID(), operationsPI);
		
		graph.associate(facultyUA.getID(), writePDS.getID(), operationsNon);
	
		
		graph.associate(tenuredUA.getID(), readPDS.getID(), operationsNon);
	

		users = new ArrayList<Long>();
		users.add(tenuredUA.getID());
		users.add(tenureTrackUA.getID());
		users.add(researchUA.getID());
		users.add(teachingUA.getID());
		users.add(clinicalUA.getID());
		users.add(adjunctUA.getID());
		users.add(PIElegibleUA.getID());




		return graph;
	}
	public Graph getGraph(){
		return graph;
	}
	public List<Long> getUsers(){
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


