package gpms.policy.customFunctions;

import gov.nist.csd.pm.epp.FunctionEvaluator;
import gov.nist.csd.pm.epp.events.EventContext;
import gov.nist.csd.pm.epp.functions.FunctionExecutor;
import gov.nist.csd.pm.exceptions.PMException;
import gov.nist.csd.pm.pdp.PDP;
import gov.nist.csd.pm.pip.graph.model.nodes.Node;
import gov.nist.csd.pm.pip.obligations.evr.EVRException;
import gov.nist.csd.pm.pip.obligations.model.functions.Function;
import gpms.policy.customEvents.DeleteCoPIEvent;
import gpms.policy.customEvents.DeleteSPEvent;

public class SPToDeleteExecutor  implements FunctionExecutor {
    @Override
    public String getFunctionName() {
        return "sp_to_delete";
    }

    @Override
    public int numParams() {
        return 0;
    }

    @Override
    public Node exec(EventContext eventCtx, String user, String process, PDP pdp, Function function, FunctionEvaluator functionEvaluator) throws PMException {
        Node sp;
        if(eventCtx instanceof DeleteSPEvent) {
        	sp = ((DeleteSPEvent) eventCtx).getSP();
        }  else {
            throw new EVRException("invalid event context for function sp_to_delete. Valid event contexts is delete-sp.");
        }
        return sp;
    }
}