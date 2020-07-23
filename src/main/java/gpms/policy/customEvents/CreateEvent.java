package gpms.policy.customEvents;

import gov.nist.csd.pm.epp.events.EventContext;
import gov.nist.csd.pm.pip.graph.model.nodes.Node;

public class CreateEvent extends EventContext {
	 public CreateEvent(Node target) {
	        super("create", target);
	    }
	 
}
