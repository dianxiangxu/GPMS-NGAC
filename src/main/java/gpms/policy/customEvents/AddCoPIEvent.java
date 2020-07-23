package gpms.policy.customEvents;

import gov.nist.csd.pm.epp.events.EventContext;
import gov.nist.csd.pm.pip.graph.model.nodes.Node;

public class AddCoPIEvent extends EventContext {
    private Node copiToAdd;

	
	 public AddCoPIEvent(Node target, Node copiToAdd) {
	        super("add-copi", target);
	        this.copiToAdd =copiToAdd; 
	    }
	 public Node getCoPI() {
	        return copiToAdd;
	    }

	    public void setCoPI(Node copiToAdd) {
	        this.copiToAdd = copiToAdd;
	    }
	 
	 }
