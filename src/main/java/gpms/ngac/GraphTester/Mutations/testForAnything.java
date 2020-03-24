package gpms.ngac.GraphTester.Mutations;

import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.UA;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import gov.nist.csd.pm.exceptions.PMException;
import gov.nist.csd.pm.pip.graph.Graph;
import gov.nist.csd.pm.pip.graph.GraphSerializer;
import gov.nist.csd.pm.pip.graph.MemGraph;
import gov.nist.csd.pm.pip.graph.model.nodes.Node;
import gpms.ngac.policy.NGACPolicyConfigurationLoader;

public class testForAnything {

	public static void main(String[] args) throws PMException {
		testForAnything test = new testForAnything();
		List<String> entities = test.getPositionTypesWithAccessByUA("PI-Eligible Faculty");
		for (String s : entities) {
			System.out.println(s);
		}
	}

	private List<String> getPositionTypesWithAccessByUA(String ua) throws PMException {
		List<String> arrayOfElegiblePIs = new ArrayList<String>();
		NGACPolicyConfigurationLoader loader = new NGACPolicyConfigurationLoader();
		loader.init();
		String proposalCreationPolicy = loader.jsonProposalCreation;
		String jsonsuper = loader.jsonSuper;
		Graph graph = GraphSerializer.fromJson(new MemGraph(), jsonsuper);

		graph = GraphSerializer.fromJson(graph, proposalCreationPolicy);
		Set<Node> piSet = graph.search(ua, "UA", null);
		Node[] piArray = piSet.toArray(new Node[graph.getNodes().size()]);
		Node piNode = piArray[0];

		Map<String, String> visited = new HashMap<String, String>();
		visited.put("isVisited", "yes");

		Stack<Node> stack = new Stack<Node>();
		stack.push(piNode);
		//System.out.println(piNode);

		while (!stack.isEmpty()) {

			Node newRoot = stack.pop();
			Set<Long> children = graph.getChildren(newRoot.getID());

			for (Long userAttNode : children) {
				Node child = graph.getNode(userAttNode);
				if (!child.getProperties().equals(visited)) {
					stack.push(child);
				}
			}
			if (newRoot.getProperties().equals(visited) || newRoot.getType() != UA) {

				continue;
			}
			if (!newRoot.getName().equals(ua)) {
				arrayOfElegiblePIs.add(newRoot.getName());
			}
			graph.updateNode(newRoot.getID(), newRoot.getName(), visited);

		}

		return arrayOfElegiblePIs;
	}
}
