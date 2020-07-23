package gpms.policy.customFunctions;

import java.util.ArrayList;
import java.util.List;

import gov.nist.csd.pm.epp.FunctionEvaluator;
import gov.nist.csd.pm.epp.events.EventContext;
import gov.nist.csd.pm.epp.functions.FunctionExecutor;
import gov.nist.csd.pm.exceptions.PMException;
import gov.nist.csd.pm.pdp.PDP;
import gov.nist.csd.pm.pip.graph.model.nodes.Node;
import gov.nist.csd.pm.pip.obligations.model.functions.Arg;
import gov.nist.csd.pm.pip.obligations.model.functions.Function;

public class ChairsForExecutor implements FunctionExecutor{
	@Override
    public String getFunctionName() {
        return "chairs_for";
    }

    @Override
    public int numParams() {
        return 1;
    }

    @Override
    public List<String> exec(EventContext eventCtx, String userID, String processID, PDP pdp, Function function, FunctionEvaluator functionEvaluator) throws PMException {
        List<Arg> args = function.getArgs();
        if (args == null || args.size() < numParams() || args.size() > numParams()) {
            throw new PMException(getFunctionName() + " expected at least one arguments (name and type) but found none");
        }

        Arg arg = args.get(0);
        Function argFunction = arg.getFunction();
        if (argFunction == null) {
            throw new PMException(getFunctionName() + " expected the first argument to be a function but it was null");
        }

        List<String> nodes = functionEvaluator.evalNodeList(eventCtx, userID, processID, pdp, argFunction);
        List<String> returnNodes = new ArrayList<String>();
        for(String node : nodes) {
        	String departmentChair = pdp.getPAP().getGraphPAP().getNode(node).getProperties().get("departmentChair");
        	returnNodes.add(departmentChair);
        }
        
                
        return returnNodes;
    }
}
