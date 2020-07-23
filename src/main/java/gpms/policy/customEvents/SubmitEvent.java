package gpms.policy.customEvents;

import gov.nist.csd.pm.epp.events.EventContext;
import gov.nist.csd.pm.pip.graph.model.nodes.Node;

public class SubmitEvent extends EventContext {
	
	Boolean irbApprovalRequired = false;
	 public SubmitEvent(Node target, Boolean irbApprovalRequired) {
	        super("submit", target);
	        this.irbApprovalRequired = irbApprovalRequired;
	    }
	 public Boolean getApprovalRequired()
	 {
		 return irbApprovalRequired;
	 } 
}
