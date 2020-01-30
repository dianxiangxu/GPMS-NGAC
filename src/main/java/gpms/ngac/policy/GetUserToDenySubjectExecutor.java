package gpms.ngac.policy;

import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.UA;
import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.U;

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
import gov.nist.csd.pm.pip.prohibitions.model.Prohibition;

public class GetUserToDenySubjectExecutor implements FunctionExecutor {

	@Override
	public String getFunctionName() {
		return "get_user_to_deny_subject";
	}

	@Override
	public int numParams() {
		return 2;
	}

	@Override
	public Prohibition.Subject exec(EventContext eventCtx, long userID, long processID, PDP pdp, Function function,
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

		// second arg should be the type of the node to search for
		arg = args.get(1);
		String type = arg.getValue();
		if (arg.getFunction() != null) {
			type = functionEvaluator.evalString(eventCtx, userID, processID, pdp, arg.getFunction());
		}

		Map<String, String> props = new HashMap<>();
		if (args.size() > 2) {
			arg = args.get(2);
			if (arg.getFunction() != null) {
				props = (Map) functionEvaluator.evalMap(eventCtx, userID, processID, pdp, arg.getFunction());
			}
		}

		Set<Node> search = pdp.getPAP().getGraphPAP().search(name, type, props);
		Node node = search.iterator().next();
		if (node.getType() == UA) {
			return new Prohibition.Subject(node.getID(), Prohibition.Subject.Type.USER_ATTRIBUTE);
		} else if (node.getType() == U) {
			return new Prohibition.Subject(node.getID(), Prohibition.Subject.Type.USER);
		}
		else {
			throw new PMException(
					getFunctionName() + " input is not correct, UA or U were expected.");
		}

	}

}
