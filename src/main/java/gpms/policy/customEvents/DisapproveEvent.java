package gpms.policy.customEvents;

import gov.nist.csd.pm.epp.events.EventContext;
import gov.nist.csd.pm.pip.graph.model.nodes.Node;

public class DisapproveEvent extends EventContext {
	 public DisapproveEvent(Node target) {
	        super("disapprove", target);
	    }
}
