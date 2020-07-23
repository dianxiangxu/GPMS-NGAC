package gpms.policy.customFunctions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import gov.nist.csd.pm.epp.FunctionEvaluator;
import gov.nist.csd.pm.epp.events.EventContext;
import gov.nist.csd.pm.epp.functions.FunctionExecutor;
import gov.nist.csd.pm.exceptions.PMException;
import gov.nist.csd.pm.pdp.PDP;
import gov.nist.csd.pm.pip.graph.model.nodes.Node;
import gov.nist.csd.pm.pip.obligations.model.functions.Arg;
import gov.nist.csd.pm.pip.obligations.model.functions.Function;

public class DeleteNodeExecutor implements FunctionExecutor {

	@Override
	public String getFunctionName() {
		return "delete_node";
	}

	@Override
	public int numParams() {
		return 1;
	}

	@Override
	public Node exec(EventContext eventCtx, String userID, String processID, PDP pdp, Function function,
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
			pdp.getPAP().getGraphPAP().deleteNode(name);
		}
		else {
			//System.out.println("Node does not exists");
		}
		return null;
	}
}
