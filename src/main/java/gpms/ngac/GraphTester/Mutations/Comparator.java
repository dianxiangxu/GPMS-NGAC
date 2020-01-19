package gpms.ngac.GraphTester.Mutations;

import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.OA;
import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.UA;
import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.U;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import gov.nist.csd.pm.exceptions.PMException;
import gov.nist.csd.pm.pip.graph.Graph;
import gov.nist.csd.pm.pip.graph.GraphSerializer;
import gov.nist.csd.pm.pip.graph.MemGraph;
import gov.nist.csd.pm.pip.graph.model.nodes.Node;

public class Comparator {

	private Graph graph1;

	private Graph graph2;



	public boolean compareGraphsAll(Graph graph1, Graph graph2) throws PMException {
		this.graph1 = graph1;
		this.graph2 = graph2;

		if (!compareAssignments()) {
			return false;
		}
		if (!compareAssociations()) {
			return false;
		}
		if (!compareNodes()) {
			return false;
		}
		return true;
	}
	public boolean compareGraphsAssoc(Graph graph1, Graph graph2) throws PMException {
		this.graph1 = graph1;
		this.graph2 = graph2;
		
		if (!compareAssociations()) {
			return false;
		}
		if (!compareNodes()) {
			return false;
		}
		return true;
	}
	private boolean compareAssignments() throws PMException {

		Node[] nodes = graph1.getNodes().toArray(
				new Node[graph1.getNodes().size()]);
		for (Node node : nodes) {
			Set<Long> parents1 = graph1.getParents(node.getID());
			if(!graph2.exists(node.getID())){
				return false;
			}
			Set<Long> parents2 = graph2.getParents(node.getID());
			for (Long parent : parents1) {
				if (!parents2.contains(parent)) {
					return false;
				}

			}
			for (Long parent : parents2) {
				if (!parents1.contains(parent)) {
					return false;
				}

			}
		}

		return true;
	}

	private boolean compareAssociations() throws PMException {

		Node[] nodes = graph1.getNodes().toArray(
				new Node[graph1.getNodes().size()]);

		for (Node node : nodes) {

			if (node.getType() == UA) {
				Map<Long, Set<String>> associations = graph1
						.getSourceAssociations(node.getID());
				if(!graph2.exists(node.getID())){
					return false;
				}
				if(node.equals(null)){System.out.println("hello");}
				Map<Long, Set<String>> associations1 = graph2
						.getSourceAssociations(node.getID());
				if (!associations.equals(associations1)) {
					return false;
				}
			}
		}

		return true;
	}
	private boolean compareNodes() throws PMException {
		Node[] nodes1 = graph1.getNodes().toArray(
				new Node[graph1.getNodes().size()]);
		Node[] nodes2 = graph2.getNodes().toArray(
				new Node[graph1.getNodes().size()]);
		
		if(nodes1.length!=nodes2.length)
		{
			return false;
		}
		for (Node node : nodes1) {
			if(!graph2.exists(node.getID())){
				return false;
			}
		}
		return true;
	}
	public boolean exists(Graph graph, List<Graph> graphs) throws PMException{
		for(Graph graphInList:graphs){
			if(compareGraphsAll(graph, graphInList)){
				return true;
			}
		}
		return false;
	}
}
