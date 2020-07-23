package gpms.policy.customFunctions;

import java.util.List;

import gov.nist.csd.pm.epp.FunctionEvaluator;
import gov.nist.csd.pm.epp.events.EventContext;
import gov.nist.csd.pm.epp.functions.FunctionExecutor;
import gov.nist.csd.pm.exceptions.PMException;
import gov.nist.csd.pm.pdp.PDP;
import gov.nist.csd.pm.pip.graph.model.nodes.Node;
import gov.nist.csd.pm.pip.obligations.model.functions.Arg;
import gov.nist.csd.pm.pip.obligations.model.functions.Function;

public class HasChildrenExecutor implements FunctionExecutor {

	@Override
	public String getFunctionName() {
		return "has_children";
	}

	@Override
	public int numParams() {
		return 1;
	}

	@Override
	public Boolean exec(EventContext eventCtx, String userID, String processID, PDP pdp, Function function,
			FunctionEvaluator functionEvaluator) throws PMException {
		List<Arg> args = function.getArgs();
		if (args == null || args.size() < numParams() || args.size() > numParams()) {
			throw new PMException(
					getFunctionName() + " expected at least one arguments (name and type) but found none");
		}

		// first arg should be a string or a function tht returns a string
		Arg arg = args.get(0);
		String name = arg.getValue();
		if (arg.getFunction() != null) {
			name = functionEvaluator.evalString(eventCtx, userID, processID, pdp, arg.getFunction());
		}
		
		if (pdp.getPAP().getGraphPAP().getChildren(name).size()>0) {
			//System.out.println("HAS_CHILDREN: "+ true);
			for(String child : pdp.getPAP().getGraphPAP().getChildren(name)) {
				//System.out.println("CHILDREN FROM HAS_CHILDREN: "+ child);
			}
			
			return true;
		} else {
			//System.out.println("HAS_CHILDREN: "+ false);
			return false;
		}
	}
}