package gpms.policy;

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
	DELETE_CO_PI,
	DELETE_SP,
	ADD_SP;
	
	public HashMap<Attribute,HashSet> getPermissionsSets()
	{
		HashMap<Attribute, HashSet> permissionsSet = new HashMap<Attribute,HashSet>();
		
		String[] operationsCreateProposal = {"create"};
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
				ops2.add("assign-u-from");
				Attribute att2 = new Attribute(Constants.COPI_ELIGIBLE_FACULTY_OA, NodeType.UA);
				permissionsSet.put(att2, new HashSet(ops2) );
				
				ArrayList<String> ops3 = new ArrayList<String>();
				ops3.add("assign-u-to");
				Attribute att3 = new Attribute(Constants.CO_PI_UA_LBL, NodeType.UA);
				permissionsSet.put(att3, new HashSet(ops3) );
				
				break;
			case DELETE_CO_PI:
				
				ArrayList<String> opsDelCopPI = new ArrayList<String>();
				opsDelCopPI.add("deassign-u-to");
				Attribute attDelCopPI = new Attribute(Constants.CO_PI_UA_LBL, NodeType.UA);
				permissionsSet.put(attDelCopPI, new HashSet(opsDelCopPI) );
				
				break;	
			case ADD_SP:
				ArrayList<String> ops4 = new ArrayList<String>();
				ops4.add("assign-u-from");
				Attribute att4 = new Attribute(Constants.SP_ELIGIBLE_FACULTY_OA, NodeType.UA);
				permissionsSet.put(att4, new HashSet(ops4) );
				
				ArrayList<String> ops5 = new ArrayList<String>();
				ops5.add("assign-u-to");
				Attribute att5 = new Attribute(Constants.SENIOR_PERSON_UA_LBL, NodeType.UA);
				permissionsSet.put(att5, new HashSet(ops5) );
				
				break;	
			case DELETE_SP:
				
				ArrayList<String> opsDelDP = new ArrayList<String>();
				opsDelDP.add("deassign-u-to");
				Attribute attDelSP = new Attribute(Constants.SENIOR_PERSON_UA_LBL, NodeType.UA);
				permissionsSet.put(attDelSP, new HashSet(opsDelDP) );
				
				break;		
		}
		
		return permissionsSet;
	}
}
