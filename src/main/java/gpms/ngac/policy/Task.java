package gpms.ngac.policy;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import gov.nist.csd.pm.pip.graph.model.nodes.NodeType;
import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.O;
import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.OA;
import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.U;
import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.UA;

public enum Task 
{
	CREATE_PROPOSAL,
	ADD_CO_PI,
	ADD_SP;
	
	public HashMap<Attribute,HashSet> getPermissionsSets()
	{
		HashMap<Attribute, HashSet> permissionsSet = new HashMap<Attribute,HashSet>();
		
		String[] operationsCreateProposal = {"create-oa"};
		String[] operationsAddCoPi_on_Faculty = {"create-o","create-o-to-oa"};
		
		switch(this)  
		{
			
			case CREATE_PROPOSAL:
				//Math.p
				ArrayList<String> ops = new ArrayList<String>();
				ops.addAll(Arrays.asList(operationsCreateProposal));
				Attribute att = new Attribute(Constants.PDS_ORIGINATING_OA, OA);
				permissionsSet.put(att, new HashSet(ops) );
				break;
			case ADD_CO_PI:
				ArrayList<String> ops2 = new ArrayList<String>();
				ops2.add("assign");
				ops2.add("assign to");
				Attribute att2 = new Attribute(Constants.PDS_ORIGINATING_OA, NodeType.OA);
				permissionsSet.put(att2, new HashSet(ops2) );
				break;
		}
		
		return permissionsSet;
	}
}
