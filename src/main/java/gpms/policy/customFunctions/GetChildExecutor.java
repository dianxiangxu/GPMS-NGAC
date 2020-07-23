package gpms.policy.customFunctions;

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

public class GetChildExecutor implements FunctionExecutor {

	@Override
	public String getFunctionName() {
		return "get_child";
	}

	@Override
	public int numParams() {
		return 1;
	}

	@Override
	public String exec(EventContext eventCtx, String userID, String processID, PDP pdp, Function function,
			FunctionEvaluator functionEvaluator) throws PMException {
		List<Arg> args = function.getArgs();
		if (args == null || args.size() < numParams() || args.size() > numParams()) {
			throw new PMException(
					getFunctionName() + " expected at least two arguments (name and type) but found none");
		}

		// first arg should be a string or a function tht returns a string
		Arg arg = args.get(0);
		String name = arg.getValue();
		if (arg.getFunction() != null) {
			name = functionEvaluator.evalString(eventCtx, userID, processID, pdp, arg.getFunction());
		}

		if (pdp.getPAP().getGraphPAP().exists(name)) {
			for (String child : pdp.getPAP().getGraphPAP().getChildren(name)) {
				if (pdp.getPAP().getGraphPAP().getNode(child).getType().toString().equals("U")) {
					return pdp.getPAP().getGraphPAP().getNode(child).getName();
				}
			}
		} else {
			throw new PMException("The node does not exist");
		}
		throw new PMException("The user child does not exist");
	}
}
