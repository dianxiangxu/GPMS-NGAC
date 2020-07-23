package gpms.policy.customFunctions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import gov.nist.csd.pm.exceptions.PMException;
import gov.nist.csd.pm.epp.FunctionEvaluator;
import gov.nist.csd.pm.epp.events.EventContext;
import gov.nist.csd.pm.epp.functions.FunctionExecutor;
import gov.nist.csd.pm.exceptions.PMException;
import gov.nist.csd.pm.pdp.PDP;
import gov.nist.csd.pm.pip.graph.model.nodes.Node;
import gov.nist.csd.pm.pip.obligations.model.functions.Arg;
import gov.nist.csd.pm.pip.obligations.model.functions.Function;

public class GetChildrenUsersInPolicyClassExecutor implements FunctionExecutor {

	@Override
	public String getFunctionName() {
		return "get_children_users_in_policy_class";
	}

	@Override
	public int numParams() {
		return 2;
	}

	@Override
	public List<String> exec(EventContext eventCtx, String userID, String processID, PDP pdp, Function function,
			FunctionEvaluator functionEvaluator) throws PMException {
		List<Arg> args = function.getArgs();
		if (args == null || args.size() < numParams() || args.size() > numParams()) {
			throw new PMException(
					getFunctionName() + " expected at least two arguments (name and type) but found none");
		}

		// first arg should be a string or a function tht returns a string
		Arg arg = args.get(0);
		Function argFunction = arg.getFunction();
		if (argFunction == null) {
			throw new PMException(getFunctionName() + " expected the first argument to be a function but it was null");
		}

		List<String> nodes = functionEvaluator.evalNodeList(eventCtx, userID, processID, pdp, argFunction);
		List<String> nodesToReturn = new ArrayList<String>();

		for (String name : nodes) {
			if (pdp.getPAP().getGraphPAP().exists(name)) {
				for (String child : pdp.getPAP().getGraphPAP().getChildren(name)) {
					if (pdp.getPAP().getGraphPAP().getNode(child).getType().toString().equals("U")) {
						nodesToReturn.add(pdp.getPAP().getGraphPAP().getNode(child).getName());
					}
				}
			} else {
				throw new PMException("The node does not exist");
			}
			throw new PMException("The user child does not exist");
		}
		return nodesToReturn; 
	}
}
