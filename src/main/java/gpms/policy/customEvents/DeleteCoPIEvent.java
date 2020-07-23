package gpms.policy.customEvents;

import gov.nist.csd.pm.epp.events.EventContext;
import gov.nist.csd.pm.pip.graph.model.nodes.Node;

public class DeleteCoPIEvent extends EventContext {
    private Node copiToDelete;

	
	 public DeleteCoPIEvent(Node target, Node copiToDelete) {
	        super("delete-copi", target);
	        this.copiToDelete =copiToDelete; 
	    }
	 public Node getCoPI() {
	        return copiToDelete;
	    }

	    public void setCoPI(Node copiToDelete) {
	        this.copiToDelete = copiToDelete;
	    }
	 
	 }