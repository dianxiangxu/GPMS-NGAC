package gpms.policy.customFunctions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import gov.nist.csd.pm.epp.FunctionEvaluator;
import gov.nist.csd.pm.epp.events.EventContext;
import gov.nist.csd.pm.epp.functions.FunctionExecutor;
import gov.nist.csd.pm.exceptions.PMException;
import gov.nist.csd.pm.pdp.PDP;
import gov.nist.csd.pm.pip.graph.Graph;
import gov.nist.csd.pm.pip.graph.model.nodes.Node;
import gov.nist.csd.pm.pip.graph.model.nodes.NodeType;
import gov.nist.csd.pm.pip.obligations.model.functions.Arg;
import gov.nist.csd.pm.pip.obligations.model.functions.Function;

public class ConcatExecutor implements FunctionExecutor {
    @Override
    public String getFunctionName() {
        return "concat_strings";
    }

    /**
     * parent name, parent type, parent properties, name, type, properties
     * @return
     */
    @Override
    public int numParams() {
        return 2;
    }

    @Override
    public String exec(EventContext eventCtx, String user, String process, PDP pdp, Function function, FunctionEvaluator functionEvaluator) throws PMException {
        List<Arg> args = function.getArgs();

        Arg string1Arg = args.get(0);
        String string1 = string1Arg.getValue();
        if(string1Arg.getFunction() != null) {
        	string1 = functionEvaluator.evalString(eventCtx, user, process, pdp, string1Arg.getFunction());
        }

        // second arg is the type, can be function
        Arg string2Arg = args.get(1);
        String string2 = string2Arg.getValue();
        if(string2Arg.getFunction() != null) {
        	string2 = functionEvaluator.evalString(eventCtx, user, process, pdp, string2Arg.getFunction());
        }

        //System.out.println("Result: "+string1+string2);
        return string1 + string2;
    }
}
