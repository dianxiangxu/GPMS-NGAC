package gpms.ngac.policy.customEvents;

import gov.nist.csd.pm.epp.events.EventContext;
import gov.nist.csd.pm.pip.graph.model.nodes.Node;

public class CreateoaEvent extends EventContext {
	 public CreateoaEvent(Node target) {
	        super("create-oa", target);
	    }
}
