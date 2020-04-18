package gpms.ngac.GraphTester.Mutations;

import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.UA;
import static gov.nist.csd.pm.pip.graph.model.nodes.Properties.NAMESPACE_PROPERTY;
import static gov.nist.csd.pm.pip.graph.model.nodes.Properties.REP_PROPERTY;
import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.PC;
import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.U;
import static gov.nist.csd.pm.pdp.decider.PReviewDecider.ALL_OPERATIONS;
import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.OA;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import gov.nist.csd.pm.exceptions.PMException;
import gov.nist.csd.pm.operations.OperationSet;
import gov.nist.csd.pm.pip.graph.Graph;
import gov.nist.csd.pm.pip.graph.GraphSerializer;
import gov.nist.csd.pm.pip.graph.MemGraph;
import gov.nist.csd.pm.pip.graph.model.nodes.Node;
import gov.nist.csd.pm.pip.graph.model.nodes.NodeType;
import gpms.ngac.policy.NGACPolicyConfigurationLoader;

public class testForAnything {
	private static final Node superUser = new Node("super", U, Node.toProperties(NAMESPACE_PROPERTY, "super"));
	private Node superUA1;
	private Node superUA2;
	private Node superPolicyClassRep;
	private Node superOA;
	private Node superPC;

	public Node getSuperUser() {
		return superUser;
	}

	public Node getSuperUserAttribute() {
		return superUA1;
	}

	public Node getSuperUserAttribute2() {
		return superUA2;
	}

	public Node getSuperPolicyClassRep() {
		return superPolicyClassRep;
	}

	public Node getSuperObjectAttribute() {
		return superOA;
	}

	public Node getSuperPolicyClass() {
		return superPC;
	}

	public static void main(String[] args) throws PMException {

//		Graph graph = new MemGraph();
//		graph.createPolicyClass("PolicyClass", null);
//		graph.createNode("a", UA, null, "PolicyClass");
//		graph.createNode("b", UA, null, "a");
//		graph.createNode("c", OA, null, "PolicyClass");
//
//		OperationSet os = new OperationSet();
//		os.add("assign-to");
//		graph.associate("a", "c", os);
//		test.configure();
		// System.out.println(GraphSerializer.toJson(graph));
//		List<String> entities = test.getPositionTypesWithAccessByUA("PI-Eligible Faculty");
//		for (String s : entities) {
//			System.out.println(s);
//		}
	}

//	private List<String> getPositionTypesWithAccessByUA(String ua) throws PMException {
//		List<String> arrayOfElegiblePIs = new ArrayList<String>();
//		NGACPolicyConfigurationLoader loader = new NGACPolicyConfigurationLoader();
//		loader.init();
//		String proposalCreationPolicy = loader.jsonProposalCreation;
//		String jsonsuper = loader.jsonSuper;
//		Graph graph = GraphSerializer.fromJson(new MemGraph(), jsonsuper);
//
//		graph = GraphSerializer.fromJson(graph, proposalCreationPolicy);
//		Set<Node> piSet = graph.search(ua, "UA", null);
//		Node[] piArray = piSet.toArray(new Node[graph.getNodes().size()]);
//		Node piNode = piArray[0];
//
//		Map<String, String> visited = new HashMap<String, String>();
//		visited.put("isVisited", "yes");
//
//		Stack<Node> stack = new Stack<Node>();
//		stack.push(piNode);
//		//System.out.println(piNode);
//
//		while (!stack.isEmpty()) {
//
//			Node newRoot = stack.pop();
//			Set<Long> children = graph.getChildren(newRoot.getID());
//
//			for (Long userAttNode : children) {
//				Node child = graph.getNode(userAttNode);
//				if (!child.getProperties().equals(visited)) {
//					stack.push(child);
//				}
//			}
//			if (newRoot.getProperties().equals(visited) || newRoot.getType() != UA) {
//
//				continue;
//			}
//			if (!newRoot.getName().equals(ua)) {
//				arrayOfElegiblePIs.add(newRoot.getName());
//			}
//			graph.updateNode(newRoot.getID(), newRoot.getName(), visited);
//
//		}
//
//		return arrayOfElegiblePIs;
//	}
	public void configure() throws PMException {
		Graph graph = new MemGraph();
		String superPCRep = "super_pc_rep";
		if (!graph.exists("super_pc")) {
			Map<String, String> props = Node.toProperties(NAMESPACE_PROPERTY, "super", REP_PROPERTY, "super_pc_rep");
			superPC = graph.createPolicyClass("super_pc", props);
		} else {
			superPC = graph.getNode("super_pc");
			superPC.getProperties().put(REP_PROPERTY, superPCRep);
			graph.updateNode(superPC.getName(), superPC.getProperties());
		}

		if (!graph.exists("super_ua1")) {
			superUA1 = graph.createNode("super_ua1", UA, Node.toProperties(NAMESPACE_PROPERTY, "super"),
					superPC.getName());
		} else {
			superUA1 = graph.getNode("super_ua1");
		}

		if (!graph.exists("super_ua2")) {
			superUA2 = graph.createNode("super_ua2", UA, Node.toProperties(NAMESPACE_PROPERTY, "super"),
					superPC.getName());
		} else {
			superUA2 = graph.getNode("super_ua2");
		}

		if (!graph.exists("super")) {
			graph.createNode("super", U, Node.toProperties(NAMESPACE_PROPERTY, "super"), superUA1.getName(),
					superUA2.getName());
		}

		if (!graph.exists("super_oa")) {
			superOA = graph.createNode("super_oa", OA, Node.toProperties(NAMESPACE_PROPERTY, "super"),
					superPC.getName());
		} else {
			superOA = graph.getNode("super_oa");
		}

		if (!graph.exists("super_pc_rep")) {
			superPolicyClassRep = graph.createNode(superPCRep, NodeType.OA,
					Node.toProperties(NAMESPACE_PROPERTY, "super", "pc", String.valueOf(superPC.getName())),
					superOA.getName());
		}

		// check super ua1 is assigned to super pc
		Set<String> children = graph.getChildren(superPC.getName());
		if (!children.contains(superUA1.getName())) {
			graph.assign(superUA1.getName(), superPC.getName());
		}
		// check super ua2 is assigned to super pc
		if (!children.contains(superUA2.getName())) {
			graph.assign(superUA2.getName(), superPC.getName());
		}
		// check super ua2 is assigned to super pc
		children = graph.getChildren(superPC.getName());
		if (!children.contains(superUA2.getName())) {
			graph.assign(superUA2.getName(), superPC.getName());
		}
		// check super user is assigned to super ua1
		children = graph.getChildren(superUA1.getName());
		if (!children.contains(superUser.getName())) {
			graph.assign(superUser.getName(), superUA1.getName());
		}
		// check super user is assigned to super ua2
		children = graph.getChildren(superUA2.getName());
		if (!children.contains(superUser.getName())) {
			graph.assign(superUser.getName(), superUA2.getName());
		}
		// check super oa is assigned to super pc
		children = graph.getChildren(superPC.getName());
		if (!children.contains(superOA.getName())) {
			graph.assign(superOA.getName(), superPC.getName());
		}
		// check super o is assigned to super oa
		children = graph.getChildren(superOA.getName());
		if (!children.contains(superPolicyClassRep.getName())) {
			graph.assign(superPolicyClassRep.getName(), superOA.getName());
		}

		// associate super ua to super oa
		graph.associate(superUA1.getName(), superOA.getName(), new OperationSet(ALL_OPERATIONS));
		graph.associate(superUA2.getName(), superUA1.getName(), new OperationSet(ALL_OPERATIONS));
		String json = GraphSerializer.toJson(graph);

		try (FileWriter writer = new FileWriter(System.getProperty("user.dir") + "/super.json");
				BufferedWriter bw = new BufferedWriter(writer)) {

			bw.write(json);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
