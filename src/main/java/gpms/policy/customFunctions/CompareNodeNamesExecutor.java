package gpms.policy.customFunctions;

import java.util.List;

import gov.nist.csd.pm.epp.FunctionEvaluator;
import gov.nist.csd.pm.epp.events.EventContext;
import gov.nist.csd.pm.epp.functions.FunctionExecutor;
import gov.nist.csd.pm.exceptions.PMException;
import gov.nist.csd.pm.pdp.PDP;
import gov.nist.csd.pm.pip.obligations.model.functions.Arg;
import gov.nist.csd.pm.pip.obligations.model.functions.Function;

public class CompareNodeNamesExecutor implements FunctionExecutor {
	@Override
	public String getFunctionName() {
		return "compare_node_names";
	}

	/**
	 * parent name, parent type, parent properties, name, type, properties
	 * 
	 * @return
	 */
	@Override
	public int numParams() {
		return 2;
	}

	@Override
	public Boolean exec(EventContext eventCtx, String user, String process, PDP pdp, Function function,
			FunctionEvaluator functionEvaluator) throws PMException {
		List<Arg> args = function.getArgs();

		Arg string1Arg = args.get(0);
		String node1name = string1Arg.getValue();
		if (string1Arg.getFunction() != null) {
			node1name = functionEvaluator.evalString(eventCtx, user, process, pdp, string1Arg.getFunction());
		}

		// second arg is the type, can be function
		Arg string2Arg = args.get(1);
		String node2name = string2Arg.getValue();
		if (string2Arg.getFunction() != null) {
			node2name = functionEvaluator.evalString(eventCtx, user, process, pdp, string2Arg.getFunction());
		}
		//System.out.println("compare_node_names result: "+  node1name.equals(node2name));
		return node1name.equals(node2name);
	}
}
