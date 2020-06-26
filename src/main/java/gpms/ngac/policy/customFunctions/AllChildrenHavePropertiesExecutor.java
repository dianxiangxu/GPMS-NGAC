package gpms.ngac.policy.customFunctions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import gov.nist.csd.pm.epp.FunctionEvaluator;
import gov.nist.csd.pm.epp.events.EventContext;
import gov.nist.csd.pm.epp.functions.FunctionExecutor;
import gov.nist.csd.pm.exceptions.PMException;
import gov.nist.csd.pm.pdp.PDP;
import gov.nist.csd.pm.pip.graph.model.nodes.Node;
import gov.nist.csd.pm.pip.obligations.model.functions.Arg;
import gov.nist.csd.pm.pip.obligations.model.functions.Function;

public class AllChildrenHavePropertiesExecutor implements FunctionExecutor {
	@Override
	public String getFunctionName() {
		return "all_children_have_properties";
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

		Arg arg = args.get(0);
		Function argFunction = arg.getFunction();
		if (argFunction == null) {
			throw new PMException(getFunctionName() + " expected the first argument to be a function but it was null");
		}

		Node node = functionEvaluator.evalNode(eventCtx, user, process, pdp, argFunction);

		Map<String, String> props = new HashMap<>();
		Arg propsArg = args.get(1);
		if (propsArg.getFunction() != null) {
			props = (Map) functionEvaluator.evalMap(eventCtx, user, process, pdp, propsArg.getFunction());
		}
		for (String nodeName : pdp.getPAP().getGraphPAP().getChildren(node.getName())) {
			Map<String, String> mapProperties = pdp.getPAP().getGraphPAP().getNode(nodeName).getProperties();
			for (String key : props.keySet()) {
				if (mapProperties.containsKey(key)) {
					continue;
				}else {
					//System.out.println("all_children_have_properties RESULT:"+ false);
					return false;
				}

			}
		}
		//System.out.println("all_children_have_properties RESULT:"+ true);

		return true;
	}
}