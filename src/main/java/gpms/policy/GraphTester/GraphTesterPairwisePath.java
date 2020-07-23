package gpms.policy.GraphTester;

import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.OA;
import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.PC;
import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.U;
import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.UA;
import gov.nist.csd.pm.pdp.decider.PReviewDecider;
import gov.nist.csd.pm.exceptions.PMException;
import gov.nist.csd.pm.pip.graph.Graph;
import gov.nist.csd.pm.pip.graph.MemGraph;
import gov.nist.csd.pm.pip.graph.model.nodes.Node;
import gpms.dataModel.GPMSCommonInfo;
import gpms.policy.Attribute;
import gpms.policy.NGACPolicyConfigurationLoader;
import gpms.policy.Task;
import gpms.policy.UserPermissionChecker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Stack;

public class GraphTesterPairwisePath extends GraphTesterPairwise {

	public GraphTesterPairwisePath() throws Exception {
		super();
	}

	public List<String[]> testGraphPairwisePC() throws PMException {
		List<String[]> data = new ArrayList<String[]>();

		Node[] nodes = graph.getNodes().toArray(
				new Node[graph.getNodes().size()]);
		UAs = new ArrayList<String>();
		for (Node node : nodes) {
			Long userID = getID();
			if (node.getType() == UA) {
				Node UA =node;
				UAs.add(UA.getName());

			}

		}

		PReviewDecider decider = new PReviewDecider(graph);
		List<Node> listOfOA = new ArrayList<Node>();
		listOfOA.add(createPDS);
		listOfOA.add(writePDS);
		listOfOA.add(readPDS);

		String[] requiredAccessRights1 = { "create-oa" };
		String[] requiredAccessRights2 = { "write" };
		String[] requiredAccessRights3 = { "read" };
		String[] requiredAccessRights4 = { "write", "read" };

		List<String[]> listOfRights = new ArrayList<String[]>();
		listOfRights.add(requiredAccessRights1);
		listOfRights.add(requiredAccessRights2);
		listOfRights.add(requiredAccessRights3);
		listOfRights.add(requiredAccessRights4);
		int[] choices = { UAs.size(), listOfRights.size() };

		boolean noShuffle = true;
		int maxGoes = 500;
		long seed = 42;
		ArrayList<int[]> list = AllPairs.generatePairs(choices, seed, maxGoes,
				!noShuffle, null, false);
		int i = 1;
		for (Node pc : PCs) {

		for(Node oa : listOfOA) {
		for (int[] temp : list) {

			Node ua = graph.getNode(UAs.get(temp[0]));
			boolean result = decider.check(UAs.get(temp[0]),"",
					oa.getName(), listOfRights.get(temp[1]));
			if (result == true) {
				//System.out.println(ua.getName() + " "
					//	+ oa.getName() + " "
						//+ listOfRights.get(temp[1])[0] + " " + result);
			data.add(new String[] { Integer.toString(i), pc.getName(), ua.getName(), oa.getName(), listOfRights.get(temp[1])[0],
					Boolean.toString(result) });
			i++;
			}
		}

	}
	}return data;}		


}