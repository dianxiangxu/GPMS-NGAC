package gpms.policy.customFunctions;

import gov.nist.csd.pm.epp.FunctionEvaluator;
import gov.nist.csd.pm.epp.events.EventContext;
import gov.nist.csd.pm.epp.functions.FunctionExecutor;
import gov.nist.csd.pm.exceptions.PMException;
import gov.nist.csd.pm.pdp.PDP;
import gov.nist.csd.pm.pip.graph.model.nodes.Node;
import gov.nist.csd.pm.pip.obligations.model.functions.Arg;
import gov.nist.csd.pm.pip.obligations.model.functions.Function;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class GetAncestorInPCExecutor implements FunctionExecutor {
	@Override
	public String getFunctionName() {
		return "get_ancestor_in_policy_class";
	}

	@Override
	public int numParams() {
		return 3;
	}

	@Override
	public Node exec(EventContext eventCtx, String user, String process, PDP pdp, Function function,
			FunctionEvaluator functionEvaluator) throws PMException {

		List<Arg> args = function.getArgs();
		if (args == null || args.size() < numParams() || args.size() > numParams()) {
			throw new PMException(
					getFunctionName() + " expected at least three arguments (name and type) but found none");
		}

		Arg arg1 = args.get(0);
		Function argFunction1 = arg1.getFunction();
		if (argFunction1 == null) {
			throw new PMException(getFunctionName() + " expected the first argument to be a function but it was null");
		}

		Node child = functionEvaluator.evalNode(eventCtx, user, process, pdp, argFunction1);

		Arg arg2 = args.get(1);
		Function argFunction2 = arg2.getFunction();
		if (argFunction2 == null) {
			throw new PMException(getFunctionName() + " expected the second argument to be a function but it was null");
		}
		Node PC = functionEvaluator.evalNode(eventCtx, user, process, pdp, argFunction2);

		Arg arg3 = args.get(2);
		int levelOfAncestory = Integer.valueOf(arg3.getValue());


		Set<String> parents = pdp.getPAP().getGraphPAP().getParents(child.getName());
		FunctionExecutor isNodeContainedInExecutor = functionEvaluator.getFunctionExecutor("is_node_contained_in");

		List<Arg> listOfArgumentsPC = new ArrayList<Arg>();
		listOfArgumentsPC.add(new Arg(PC.getName()));
		listOfArgumentsPC.add(new Arg(PC.getType().toString()));
		Function getNode2 = new Function("get_node", listOfArgumentsPC);

		int count = 1;

		String parentVariable="";
		for (String parent : parents) {
			Node parentForChild = pdp.getPAP().getGraphPAP().getNode(parent);
			List<Arg> listOfArgumentsParent = new ArrayList<Arg>();
			listOfArgumentsParent.add(new Arg(parentForChild.getName()));
			listOfArgumentsParent.add(new Arg(parentForChild.getType().toString()));
			Function getNode1 = new Function("get_node", listOfArgumentsParent);

			List<Arg> listOfArgumentsFinal = new ArrayList<Arg>();

			listOfArgumentsFinal.add(new Arg(getNode1));
			listOfArgumentsFinal.add(new Arg(getNode2));
			Function isNodeContainedIn = new Function("is_node_contained_in", listOfArgumentsFinal);

			//function.setArgs(listOfArgumentsFinal);

			Boolean isContainedIn = (Boolean) isNodeContainedInExecutor.exec(eventCtx, user, process, pdp, isNodeContainedIn,
					functionEvaluator);

			if (count == levelOfAncestory && isContainedIn) {
				return parentForChild;
			} else if (isContainedIn) {
				if (pdp.getPAP().getGraphPAP().getParents(parentForChild.getName()).iterator().hasNext()) {
					parentVariable = parentForChild.getName();
					break;
				} else {
					continue;
				}
			}
		}
		

		while(true) {
			count++;
			String ancestor = pdp.getPAP().getGraphPAP().getParents(parentVariable).iterator().next();

			
			List<Arg> listOfArgumentsAncestor = new ArrayList<Arg>();
			listOfArgumentsAncestor.add(new Arg(ancestor));
			listOfArgumentsAncestor.add(new Arg(pdp.getPAP().getGraphPAP().getNode(ancestor).getType().toString()));
			Function getNode1 = new Function("get_node", listOfArgumentsAncestor);
			List<Arg> listOfArgumentsFinal = new ArrayList<Arg>();

			listOfArgumentsFinal.add(new Arg(getNode1));
			listOfArgumentsFinal.add(new Arg(getNode2));
			Function isNodeContainedIn = new Function("is_node_contained_in", listOfArgumentsFinal);

		//	function.setArgs(listOfArgumentsFinal);
			Boolean isContainedIn = (Boolean) isNodeContainedInExecutor.exec(eventCtx, user, process, pdp, isNodeContainedIn,
					functionEvaluator);
			
			if(count == levelOfAncestory&&isContainedIn) {		
				//System.out.println("get_ancestor_in_policy_class ancestor: "+ancestor);
				return pdp.getPAP().getGraphPAP().getNode(ancestor);
			}
			else if(isContainedIn) {
				parentVariable = ancestor;
				continue;
			}
			else {
				throw new PMException("The assumption was that one parent can only have one child in this hierarchy");
			}
		}

	}
}
