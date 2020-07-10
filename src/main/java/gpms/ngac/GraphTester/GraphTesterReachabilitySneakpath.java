package gpms.ngac.GraphTester;

import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.OA;
import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.PC;
import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.U;
import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.UA;
import gov.nist.csd.pm.pdp.decider.PReviewDecider;
import gov.nist.csd.pm.exceptions.PMException;
import gov.nist.csd.pm.pip.graph.Graph;
import gov.nist.csd.pm.pip.graph.GraphSerializer;
import gov.nist.csd.pm.pip.graph.MemGraph;
import gov.nist.csd.pm.pip.graph.model.nodes.Node;
import gpms.model.GPMSCommonInfo;
import gpms.ngac.policy.Attribute;
import gpms.ngac.policy.NGACPolicyConfigurationLoader;
import gpms.ngac.policy.Task;
import gpms.ngac.policy.UserPermissionChecker;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Stack;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

public class GraphTesterReachabilitySneakpath extends GraphTesterRS {

	int COUNTER = 0;

	public GraphTesterReachabilitySneakpath() throws PMException, Exception {
		super();

	}

	public GraphTesterReachabilitySneakpath(Graph graph) throws PMException, Exception {
		super(graph);

	}

	public void testGraphPC(List<Long> users) throws PMException {
		Node[] nodes = graph.getNodes().toArray(new Node[graph.getNodes().size()]);
		for (Node node : nodes) {
			Long userID = getID();
			if (node.getType() == UA && users.contains(node.getName())) {
				graph.createNode(node.getName() + "U", U, null, node.getName());
			}

		}
		boolean result = false;
		PReviewDecider decider = new PReviewDecider(graph);
		String[] requiredAccessRights1 = { "create-oa" };
		String[] requiredAccessRights2 = { "write" };
		List<String[]> list = new ArrayList<String[]>();
		list.add(requiredAccessRights1);
		list.add(requiredAccessRights2);
		Map<String, String> visited = new HashMap<String, String>();
		visited.put("isVisited", "yes");

		Stack<Node> stack = new Stack<Node>();
		stack.push(policy);
		while (!stack.isEmpty()) {

			Node newRoot = stack.pop();
			Set<String> set = graph.getChildren(newRoot.getName());

			for (String userAttNode : set) {
				Node child = graph.getNode(userAttNode);
				if (!child.getProperties().equals(visited)) {
					stack.push(child);
				}
			}
			if (newRoot.getProperties().equals(visited) || newRoot.getType() != U) {

				continue;
			}
			for (String[] requiredAccessRights : list) {
				if (decider.check(newRoot.getName(), "", createPDS.getName(), requiredAccessRights)) {
					result = true;
				} else {
					result = false;
				}
				//System.out.println(newRoot + " " + result + " " + createPDS.getName() + " " + requiredAccessRights[0]);
			}
			for (String[] requiredAccessRights : list) {
				if (decider.check(newRoot.getName(), "", writePDS.getName(), requiredAccessRights)) {
					result = true;
				} else {
					result = false;
				}
				//System.out.println(newRoot + " " + writePDS.getName() + " " + requiredAccessRights[0] + " " + result);
			}
			graph.updateNode(newRoot.getName(), visited);

		}

	}

	public List<String[]> testGraphPC() throws PMException, IOException {
		Node[] nodes = graph.getNodes().toArray(new Node[graph.getNodes().size()]);
		List<String[]> data = new ArrayList<String[]>();

		boolean result = false;
		PReviewDecider decider = new PReviewDecider(graph);
		String[] requiredAccessRights1 = { "create-oa" };
		String[] requiredAccessRights2 = { "write" };
		String[] requiredAccessRights3 = { "read" };
		String[] requiredAccessRights4 = { "create-oa-to-oa" };

		List<String[]> list = new ArrayList<String[]>();
		list.add(requiredAccessRights1);
		list.add(requiredAccessRights2);
		list.add(requiredAccessRights3);
		list.add(requiredAccessRights4);

		Map<String, String> visited = new HashMap<String, String>();
		visited.put("isVisited", "yes");

		if (PCs.size() == 0) {
			return data;
		}
		int i = 1;
		for (Node pc : PCs) {
			Stack<Node> stack = new Stack<Node>();
			stack.push(pc);
			//System.out.println(pc);
			if (pc == null)
				return null;
			while (!stack.isEmpty()) {
				Node newRoot = stack.pop();
				Set<String> set = graph.getChildren(newRoot.getName());

				for (String userAttNode : set) {
					Node child = graph.getNode(userAttNode);
					if (!child.getProperties().equals(visited)) {
						stack.push(child);
						continue;
					}
				}
				if (newRoot.getType() != UA) {
					continue;
				}

				for (Node oa : OAs) {

					for (String[] requiredAccessRights : list) {
						for (String AR : requiredAccessRights) {
							if (decider.check(newRoot.getName(), "", oa.getName(), AR)) {
								result = true;
							} else {
								result = false;
							}
							//System.out.println(
							//		pc.getName() + " " + newRoot + " " + oa.getName() + " " + AR + " " + result);
							data.add(new String[] {Integer.toString(i),pc.getName(), newRoot.getName(), 
									oa.getName(), AR,Boolean.toString(result)});
							i++;
						}
					}
				}
				graph.updateNode(newRoot.getName(), visited);

			}

		}
		return data;

	}
}
