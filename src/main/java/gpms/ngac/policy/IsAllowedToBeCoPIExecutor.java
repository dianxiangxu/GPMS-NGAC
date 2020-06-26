package gpms.ngac.policy;

import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.UA;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;

import gov.nist.csd.pm.epp.FunctionEvaluator;
import gov.nist.csd.pm.epp.events.AssignEvent;
import gov.nist.csd.pm.epp.events.AssignToEvent;
import gov.nist.csd.pm.epp.events.DeassignEvent;
import gov.nist.csd.pm.epp.events.DeassignFromEvent;
import gov.nist.csd.pm.epp.events.EventContext;
import gov.nist.csd.pm.epp.functions.FunctionExecutor;
import gov.nist.csd.pm.exceptions.PMException;
import gov.nist.csd.pm.pdp.PDP;
import gov.nist.csd.pm.pip.graph.Graph;
import gov.nist.csd.pm.pip.graph.GraphSerializer;
import gov.nist.csd.pm.pip.graph.MemGraph;
import gov.nist.csd.pm.pip.graph.model.nodes.Node;
import gov.nist.csd.pm.pip.obligations.evr.EVRException;
import gov.nist.csd.pm.pip.obligations.model.functions.Function;

public class IsAllowedToBeCoPIExecutor implements FunctionExecutor{
	 @Override
	    public String getFunctionName() {
	        return "isAllowedToBeCoPI";
	    }

	    @Override
	    public int numParams() {
	        return 1;
	    }

	    @Override
	    public Boolean exec(EventContext eventCtx, String userID, String processID, PDP pdp, Function function, FunctionEvaluator functionEvaluator) throws PMException {
	    	 Node child;
	         if(eventCtx instanceof AssignToEvent) {
	             child = ((AssignToEvent) eventCtx).getChildNode();
	         } else if (eventCtx instanceof AssignEvent) {
	             child = eventCtx.getTarget();
	         } else if (eventCtx instanceof DeassignFromEvent) {
	             child = ((DeassignFromEvent) eventCtx).getChildNode();
	         } else if (eventCtx instanceof DeassignEvent) {
	             child = eventCtx.getTarget();
	         } else {
	             throw new EVRException("invalid event context for function child_of_assign. Valid event contexts are AssignTo, " +
	                     "Assign, DeassignFrom, and Deassign");
	         }
	         
	         Boolean result = isAllowedToBeCoPI("CoPI-Eligible Faculty",child, pdp.getPAP().getGraphPAP());
	         if(result) return true;
	         else {
	        	 throw new PMException("This entity cannot be CoPI and the proposal can't be saved!");

	         }
	    }
	    private Boolean isAllowedToBeCoPI(String ua, Node childFromObligation,Graph graph) throws PMException {
			List<String> arrayOfElegiblePIs = new ArrayList<String>();
//			NGACPolicyConfigurationLoader loader = new NGACPolicyConfigurationLoader();
//			loader.init();
//			String proposalCreationPolicy = loader.jsonProposalCreation;
//			String jsonsuper = loader.jsonSuper;
//			Graph graph = GraphSerializer.fromJson(new MemGraph(), jsonsuper);
//
//			graph = GraphSerializer.fromJson(graph, proposalCreationPolicy);
//			Set<Node> piSet = graph.search(ua, "UA", null);
//			Node[] piArray = piSet.toArray(new Node[graph.getNodes().size()]);
//			Node piNode = piArray[0];

			Map<String, String> visited = new HashMap<String, String>();
			visited.put("isVisited", "yes");

			Stack<Node> stack = new Stack<Node>();
			stack.push(childFromObligation);
			////System.out.println(piNode);

			while (!stack.isEmpty()) {

				Node newRoot = stack.pop();
				Set<String> children = graph.getParents(newRoot.getName());

				for (String userAttNode : children) {
					Node child = graph.getNode(userAttNode);
					if (!child.getProperties().equals(visited)) {
						stack.push(child);
					}
				}
				if (newRoot.getProperties().equals(visited) || newRoot.getType() != UA) {

					continue;
				}
				if(newRoot.getName().equals(ua)) {
					//System.out.println("ALLOWED!!!!!!!!!!!!!!!");
					return true;
				}

				if (!newRoot.getName().equals(ua)) {
					//System.out.println(newRoot.getName());

					arrayOfElegiblePIs.add(newRoot.getName());
				}
				graph.updateNode(newRoot.getName(), visited);
			}

			return false;
		}
}
