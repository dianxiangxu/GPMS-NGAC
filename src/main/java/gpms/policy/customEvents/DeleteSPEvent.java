package gpms.policy.customEvents;

import gov.nist.csd.pm.epp.events.EventContext;
import gov.nist.csd.pm.pip.graph.model.nodes.Node;

public class DeleteSPEvent extends EventContext {
    private Node spToDelete;

	
	 public DeleteSPEvent(Node target, Node spToDelete) {
	        super("delete-sp", target);
	        this.spToDelete =spToDelete; 
	    }
	 public Node getSP() {
	        return spToDelete;
	    }

	    public void setSP(Node spToDelete) {
	        this.spToDelete = spToDelete;
	    }
	 
	 }
