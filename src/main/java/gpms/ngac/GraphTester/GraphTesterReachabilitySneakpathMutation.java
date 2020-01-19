package gpms.ngac.GraphTester;

import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.U;
import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.UA;
import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.OA;
import gov.nist.csd.pm.exceptions.PMException;
import gov.nist.csd.pm.pdp.decider.PReviewDecider;
import gov.nist.csd.pm.pip.graph.Graph;
import gov.nist.csd.pm.pip.graph.model.nodes.Node;
import gov.nist.csd.pm.pip.prohibitions.MemProhibitions;
import gov.nist.csd.pm.pip.prohibitions.Prohibitions;
import gov.nist.csd.pm.pip.prohibitions.ProhibitionsSerializer;
import gov.nist.csd.pm.pip.prohibitions.model.Prohibition;
import gov.nist.csd.pm.pip.prohibitions.model.Prohibition.Subject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

public class GraphTesterReachabilitySneakpathMutation extends GraphTesterRS {
	Prohibitions prohibitions = new MemProhibitions();
	PReviewDecider decider = new PReviewDecider(graph);

	public GraphTesterReachabilitySneakpathMutation(Graph graph, Node root)
			throws PMException, Exception {
		super(graph, root);
		this.graph = graph;

		Set<Node> set = graph.search("createPDS", "OA",
				new HashMap<String, String>());
		Node[] array = set.toArray(new Node[graph.getNodes().size()]);
		policy = root;
		createPDS = array[0];
		// testGraphPC();
	}

	public GraphTesterReachabilitySneakpathMutation() throws PMException,
			Exception {
		super();

	}

	public void mutator() throws PMException {
		Node[] nodes = graph.getNodes().toArray(
				new Node[graph.getNodes().size()]);
		List<Node> oas = new ArrayList<Node>();
		for (Node node : nodes) {

			if (node.getType() == OA) {
				oas.add(node);
			}
		}
		for(Node oa : oas){
			Map<Long, Set<String>> associations = graph
					.getTargetAssociations(oa.getID());
			Set<String> operations = null;
			for (long objectID : associations.keySet()) {

				Node ua = graph.getNode(objectID);	
			    operations = associations.get(ua.getID());
				graph.dissociate(ua.getID(), oa.getID());

			}
			makeMutations(nodes, oa, operations);

			
		}
		
		for(Prohibition p : prohibitions.getAll()){
			System.out.println(p.getName()+" " +" " + graph.getNode(p.getSubject().getSubjectID()).getName());
		}
		String json = ProhibitionsSerializer.toJson(prohibitions);
		System.out.println(json);
		
	}

	private void makeMutations(Node[] nodes, Node oa, Set<String> operations)
			throws PMException {

		
		for (Node node : nodes) {

			if (node.getType() == UA) {	
				graph.associate(node.getID(), oa.getID(), operations);
				
				Map<Long, Set<String>> associations = graph
						.getTargetAssociations(oa.getID());
				for (long objectID : associations.keySet()) {
					System.out.println(oa.getName());
					Node obj = graph.getNode(objectID);
					System.out.println("\t" + obj.getName() + "("
							+ obj.getType().toString() + ") -> "
							+ associations.get(objectID));
					testGraphPC();
				}
				//graph.dissociate(node.getID(), oa.getID());
				Subject subject = new Subject(node.getID(),Subject.Type.toType("USER_ATTRIBUTE"));
				List<Prohibition.Node> nodesForPhohib = new ArrayList<Prohibition.Node>();
				Prohibition.Node nodeForProhib = new Prohibition.Node(node.getID(), true);
				nodesForPhohib.add(nodeForProhib);
				Prohibition prohibition = new Prohibition("prohibition" + node.getName(), subject,nodesForPhohib , operations, true);
				prohibitions.add(prohibition);
				decider = new PReviewDecider(graph,prohibitions);
			}			
		}
		

	}

	public void testGraphPC() throws PMException {
		Node[] nodes = graph.getNodes().toArray(
				new Node[graph.getNodes().size()]);

		for (Node node : nodes) {
			Long userID = getID();
			if (node.getType() == UA) {
				graph.createNode(userID, node.getName() + "U", U, null);
				graph.assign(userID, node.getID());
			}

		}
		boolean result = false;
		String[] requiredAccessRights1 = { "create-oa" };
		//String[] requiredAccessRights2 = { "write" };
		List<String[]> list = new ArrayList<String[]>();
		list.add(requiredAccessRights1);
		//list.add(requiredAccessRights2);

		Map<String, String> visited = new HashMap<String, String>();
		visited.put("isVisited", "yes");

		Stack<Node> stack = new Stack<Node>();
		stack.push(policy);
		System.out.println("\t" +"\t"+policy);

		while (!stack.isEmpty()) {
			Node newRoot = stack.pop();
			Set<Long> set = graph.getChildren(newRoot.getID());

			for (Long userAttNode : set) {
				Node child = graph.getNode(userAttNode);
				if (!child.getProperties().equals(visited)) {
					stack.push(child);
					continue;
				}
			}
			if (newRoot.getType() != U) {
				continue;
			}

			for (String[] requiredAccessRights : list) {
				if (decider.check(newRoot.getID(), 102L, createPDS.getID(),
						requiredAccessRights)) {
					result = true;
				} else {
					result = false;
				}
				System.out.println("\t" +"\t" +newRoot + " " + result + " "
						+ createPDS.getName() + " " + requiredAccessRights[0]);
			}
			/*for (String[] requiredAccessRights : list) {
				if (decider.check(newRoot.getID(), 102L, writePDS.getID(),
						requiredAccessRights)) {
					result = true;
				} else {
					result = false;
				}
				System.out.println("\t" +"\t" +newRoot + " " + result + " "
						+ writePDS.getName() + " " + requiredAccessRights[0]);
			}*/
			graph.updateNode(newRoot.getID(), newRoot.getName(), visited);

		}
		
	}

}
