package gpms.policy.customEvents;

import gov.nist.csd.pm.epp.events.EventContext;
import gov.nist.csd.pm.pip.graph.model.nodes.Node;

public class AddSPEvent  extends EventContext {
    private Node spToAdd;

	
	 public AddSPEvent(Node target, Node spToAdd) {
	        super("add-sp", target);
	        this.spToAdd =spToAdd; 
	    }
	 public Node getSP() {
	        return spToAdd;
	    }

	    public void setSP(Node spToAdd) {
	        this.spToAdd = spToAdd;
	    }
	 
	 }