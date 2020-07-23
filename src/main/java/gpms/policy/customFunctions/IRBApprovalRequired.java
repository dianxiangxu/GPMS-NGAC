package gpms.policy.customFunctions;

import gov.nist.csd.pm.epp.FunctionEvaluator;
import gov.nist.csd.pm.epp.events.EventContext;
import gov.nist.csd.pm.epp.functions.FunctionExecutor;
import gov.nist.csd.pm.exceptions.PMException;
import gov.nist.csd.pm.pdp.PDP;
import gov.nist.csd.pm.pip.graph.model.nodes.Node;
import gov.nist.csd.pm.pip.obligations.evr.EVRException;
import gov.nist.csd.pm.pip.obligations.model.functions.Function;
import gpms.policy.customEvents.SubmitEvent;

public class IRBApprovalRequired implements FunctionExecutor {
    @Override
    public String getFunctionName() {
        return "irb_approval_required";
    }

    @Override
    public int numParams() {
        return 0;
    }

    @Override
    public Boolean exec(EventContext eventCtx, String user, String process, PDP pdp, Function function, FunctionEvaluator functionEvaluator) throws PMException {
        Boolean ApprovalRequired;
        if(eventCtx instanceof SubmitEvent) {
        	ApprovalRequired = ((SubmitEvent) eventCtx).getApprovalRequired();
        }  else {
            throw new EVRException("invalid event context for function copi_to_add. Valid event contexts is add-copi.");
        }
                
        return ApprovalRequired;
    }
}
