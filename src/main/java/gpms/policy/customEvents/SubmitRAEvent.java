package gpms.policy.customEvents;

import gov.nist.csd.pm.epp.events.EventContext;
import gov.nist.csd.pm.pip.graph.model.nodes.Node;

public class SubmitRAEvent extends EventContext {
	
	 public SubmitRAEvent(Node target) {
	        super("RAsubmit", target);
	    }

}
