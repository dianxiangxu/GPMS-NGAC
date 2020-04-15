package gpms.ngac.policy.customEvents;

import gov.nist.csd.pm.epp.events.EventContext;
import gov.nist.csd.pm.pip.graph.model.nodes.Node;

public class SubmitEvent extends EventContext {
	 public SubmitEvent(Node target) {
	        super("submit", target);
	    }
}
