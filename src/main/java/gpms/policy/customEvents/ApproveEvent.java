package gpms.policy.customEvents;

import gov.nist.csd.pm.epp.events.EventContext;
import gov.nist.csd.pm.pip.graph.model.nodes.Node;

public class ApproveEvent extends EventContext {
	
	
	 public ApproveEvent(Node target) {
	        super("approve", target);
	    }
	 
	 
	 }
