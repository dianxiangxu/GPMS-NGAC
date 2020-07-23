package gpms.policy.customFunctions;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import gov.nist.csd.pm.epp.FunctionEvaluator;
import gov.nist.csd.pm.epp.events.EventContext;
import gov.nist.csd.pm.epp.functions.FunctionExecutor;
import gov.nist.csd.pm.exceptions.PMException;
import gov.nist.csd.pm.pdp.PDP;
import gov.nist.csd.pm.pip.graph.dag.searcher.DepthFirstSearcher;
import gov.nist.csd.pm.pip.graph.dag.searcher.Direction;
import gov.nist.csd.pm.pip.graph.dag.visitor.Visitor;
import gov.nist.csd.pm.pip.graph.model.nodes.Node;
import gov.nist.csd.pm.pip.obligations.model.functions.Arg;
import gov.nist.csd.pm.pip.obligations.model.functions.Function;

public class IsNodeInListExecutor implements FunctionExecutor {
	@Override
	public String getFunctionName() {
		return "is_node_in_list";
	}

	@Override
	public int numParams() {
		return 2;
	}

	@Override
	public Boolean exec(EventContext eventCtx, String user, String process, PDP pdp, Function function,
			FunctionEvaluator functionEvaluator) throws PMException {
		List<Arg> args = function.getArgs();
		if (args.size() != numParams()) {
			throw new PMException(
					getFunctionName() + " expected " + numParams() + " parameters but got " + args.size());
		}
		for(String s : pdp.getPAP().getGraphPAP().getChildren("CoPI")) {
			//System.out.println("OBLIGATION CHILDREN:"+ s);
		}
		Arg arg = args.get(1);
		Function f = arg.getFunction();
		if (f == null) {
			throw new PMException(getFunctionName() + " expects two functions as parameters");
		}

		List<String> list = functionEvaluator.evalNodeList(eventCtx, user, process, pdp, f);
		if (list == null) {
			return false;
		}
		arg = args.get(0);
		f = arg.getFunction();
		if (f == null) {
			throw new PMException(getFunctionName() + " expects two functions as parameters");
		}

		Node node = functionEvaluator.evalNode(eventCtx, user, process, pdp, f);
		if (node == null) {
			return false;
		}
		//System.out.println("is_node_in_list: "+list.contains(node.getName()));
		return list.contains(node.getName());
	}
}
