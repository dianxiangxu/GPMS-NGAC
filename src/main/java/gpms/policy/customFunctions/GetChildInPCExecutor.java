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

public class GetChildInPCExecutor implements FunctionExecutor {
	@Override
	public String getFunctionName() {
		return "get_child_user_in_policy_class";
	}

	@Override
	public int numParams() {
		return 2;
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

		Node parent = functionEvaluator.evalNode(eventCtx, user, process, pdp, argFunction1);

		Arg arg2 = args.get(1);
		Function argFunction2 = arg2.getFunction();
		if (argFunction2 == null) {
			throw new PMException(getFunctionName() + " expected the second argument to be a function but it was null");
		}
		Node PC = functionEvaluator.evalNode(eventCtx, user, process, pdp, argFunction2);

		


		Set<String> children = pdp.getPAP().getGraphPAP().getChildren(parent.getName());
		FunctionExecutor isNodeContainedInExecutor = functionEvaluator.getFunctionExecutor("is_node_contained_in");

		List<Arg> listOfArgumentsPC = new ArrayList<Arg>();
		listOfArgumentsPC.add(new Arg(PC.getName()));
		listOfArgumentsPC.add(new Arg(PC.getType().toString()));
		Function getNode2 = new Function("get_node", listOfArgumentsPC);


		String parentVariable="";
		for (String child : children) {
			Node childFromParent = pdp.getPAP().getGraphPAP().getNode(child);
			List<Arg> listOfArgumentsParent = new ArrayList<Arg>();
			listOfArgumentsParent.add(new Arg(childFromParent.getName()));
			listOfArgumentsParent.add(new Arg(childFromParent.getType().toString()));
			Function getNode1 = new Function("get_node", listOfArgumentsParent);

			List<Arg> listOfArgumentsFinal = new ArrayList<Arg>();

			listOfArgumentsFinal.add(new Arg(getNode1));
			listOfArgumentsFinal.add(new Arg(getNode2));
			Function isNodeContainedIn = new Function("is_node_contained_in", listOfArgumentsFinal);

			//function.setArgs(listOfArgumentsFinal);

			Boolean isContainedIn = (Boolean) isNodeContainedInExecutor.exec(eventCtx, user, process, pdp, isNodeContainedIn,
					functionEvaluator);

			if (isContainedIn&&childFromParent.getType().toString().equals("U")) {
				//System.out.println("get_child_in_policy_class CHILD: "+childFromParent.getName());
				return childFromParent;
			} 
		}
		return null;
	}
}
