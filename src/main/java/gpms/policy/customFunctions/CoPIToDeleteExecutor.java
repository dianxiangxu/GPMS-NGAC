package gpms.policy.customFunctions;

import gov.nist.csd.pm.epp.FunctionEvaluator;
import gov.nist.csd.pm.epp.events.EventContext;
import gov.nist.csd.pm.epp.functions.FunctionExecutor;
import gov.nist.csd.pm.exceptions.PMException;
import gov.nist.csd.pm.pdp.PDP;
import gov.nist.csd.pm.pip.graph.model.nodes.Node;
import gov.nist.csd.pm.pip.obligations.evr.EVRException;
import gov.nist.csd.pm.pip.obligations.model.functions.Function;
import gpms.policy.customEvents.AddCoPIEvent;
import gpms.policy.customEvents.DeleteCoPIEvent;

public class CoPIToDeleteExecutor implements FunctionExecutor {
    @Override
    public String getFunctionName() {
        return "copi_to_delete";
    }

    @Override
    public int numParams() {
        return 0;
    }

    @Override
    public Node exec(EventContext eventCtx, String user, String process, PDP pdp, Function function, FunctionEvaluator functionEvaluator) throws PMException {
        Node copi;
        if(eventCtx instanceof DeleteCoPIEvent) {
        	copi = ((DeleteCoPIEvent) eventCtx).getCoPI();
        }  else {
            throw new EVRException("invalid event context for function copi_to_delete. Valid event contexts is delete-copi.");
        }
        return copi;
    }
}