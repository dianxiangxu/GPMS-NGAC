package gpms.dev;

import gov.nist.csd.pm.exceptions.PMException;
import gov.nist.csd.pm.graph.Graph;
import gov.nist.csd.pm.graph.model.nodes.Node;
import gpms.pds.Constants;

import java.util.*;

import static gov.nist.csd.pm.graph.model.nodes.NodeType.*;
import static gpms.dev.PDS.getID;
import static gpms.dev.PDS.getNodeID;

/**
 * This class is intended to mimic the functionality of obligations through manipulation of a graph.
 */
public class Obligations {

    /**
     * Obligation to create a new PDS, the given user is the PI, the given node is the base node for the PDS.
     * Some things are hard-coded in this example, so there will be errors with more than one PDS.
     * @param graph
     * @param userID
     * @param iPdsNode
     * @throws PMException
     */
    public static void createPDS(Graph graph, long userID, Node iPdsNode) throws PMException {
        // get the node for the user
        Node userNode = graph.getNode(userID);

        // create the object attribute hierarchy

        // create the investigator info container and assign it to the PDS
        Node investigatorInfoNode = graph.createNode(getID(), Constants.INVESTIGATOR_OA_UA_LBL, OA, null);
        graph.assign(investigatorInfoNode.getID(), iPdsNode.getID());

        // create the PI container and assign it to InvestigatorInfo
        Node piNode = graph.createNode(getID(), "PI", OA, null);
        graph.assign(piNode.getID(), investigatorInfoNode.getID());

        // create a node with the name of the user and assign it to the PI container
        Node userObj = graph.createNode(getID(), userNode.getName(), O, null);
        graph.assign(userObj.getID(), piNode.getID());

        // create the CoPI container and assign it to InvestigatorInfo
        Node copiNode = graph.createNode(getID(), "CoPI", OA, null);
        graph.assign(copiNode.getID(), investigatorInfoNode.getID());

        // create the SP container and assign it to InvestigatorInfo
        Node spNode = graph.createNode(getID(), "SP", OA, null);
        graph.assign(spNode.getID(), investigatorInfoNode.getID());

        // create the ProjectInfo container and assign it to the PDS
        Node projInfoNode = graph.createNode(getID(), "ProjectInfo", OA, null);
        graph.assign(projInfoNode.getID(), iPdsNode.getID());

        // create the BudgetInfo container and assign it to the PDS
        Node budgInfoNode = graph.createNode(getID(), "BudgetInfo", OA, null);
        graph.assign(budgInfoNode.getID(), iPdsNode.getID());

        long pdssNode = getNodeID(graph, "PDSs", OA, null);
        graph.assign(iPdsNode.getID(), pdssNode);

        // now create user attribute hierarchy

        // create a base user attribute for the PDS, assign it to a user attribute in the PDS policy class
        long pdsUA = getNodeID(graph, "PDSs", UA, null);
        Node pdsiUA = graph.createNode(getID(), "PDSi", UA, null);
        graph.assign(pdsiUA.getID(), pdsUA);

        // create a user attribute for the PI and assign it to the PDSi base user attribute
        Node piUA = graph.createNode(getID(), "PI", UA, null);
        graph.assign(piUA.getID(), pdsiUA.getID());

        // create a user attribute for the CoPI and assign it to the PDSi base user attribute
        Node copiUA = graph.createNode(getID(), "CoPI", UA, null);
        graph.assign(copiUA.getID(), pdsiUA.getID());

        // create a user attribute for the SP and assign it to the PDSi base user attribute
        Node spUA = graph.createNode(getID(), "SP", UA, null);
        graph.assign(spUA.getID(), pdsiUA.getID());

        // assign the current user to the PI user attribute
        graph.assign(userNode.getID(), piUA.getID());

        // grant the PI permissions on the PDS
        // grant PI permissions on the CoPI container
        graph.associate(piUA.getID(), copiNode.getID(), new HashSet<>(Arrays.asList("write", "read", "assign to")));
        // grant PI permissions on the ProjectInfo container
        graph.associate(piUA.getID(), projInfoNode.getID(), new HashSet<>(Arrays.asList("write", "read", "assign to")));
        // grant PI permissions on the BudgetInfo container
        graph.associate(piUA.getID(), budgInfoNode.getID(), new HashSet<>(Arrays.asList("write", "read", "assign to")));
        // grant PI permissions to assign the PDS
        graph.associate(piUA.getID(), iPdsNode.getID(), new HashSet<>(Arrays.asList("assign")));
        // grant PI permission to read the PI container
        graph.associate(piUA.getID(), piNode.getID(), new HashSet<>(Arrays.asList("read")));

        // grant CoPI permissions on the PDS
        // grant CoPI permissions on the SP container
        graph.associate(copiUA.getID(), spNode.getID(), new HashSet<>(Arrays.asList("write", "read", "assign to")));
        // grant CoPI permissions on the ProjectInfo container
        graph.associate(copiUA.getID(), projInfoNode.getID(), new HashSet<>(Arrays.asList("write", "read", "assign to")));
        // grant CoPI permissions on the BudgetInfo container
        graph.associate(copiUA.getID(), budgInfoNode.getID(), new HashSet<>(Arrays.asList("write", "read", "assign to")));

        // grant SP permissions on the PDS
        // grant SP permissions on the InvestigatorInfo container
        graph.associate(spUA.getID(), investigatorInfoNode.getID(), new HashSet<>(Arrays.asList("read")));
        // grant SP permissions on the ProjectInfo container
        graph.associate(spUA.getID(), projInfoNode.getID(), new HashSet<>(Arrays.asList("read")));
        // grant SP permissions on the BudgetInfo container
        graph.associate(spUA.getID(), budgInfoNode.getID(), new HashSet<>(Arrays.asList("read")));
    }

    /**
     * When an object is added to the CoPI container get the user with the same name and assign that user to the CoPI user attribute.
     */
    public static void addCoPI(Graph graph, Node copiNode) throws PMException {
        long user = getNodeID(graph, copiNode.getName(), U, null);
        long ua = getNodeID(graph, "CoPI", UA, null);

        graph.assign(user, ua);
    }

    /**
     * When an object is added to the SP container get the user with the same name and assign that user to the SP user attribute.
     */
    public static void addSP(Graph graph, Node spNode) throws PMException {
        long user = getNodeID(graph, spNode.getName(), U, null);
        long ua = getNodeID(graph, "SP", UA, null);

        graph.assign(user, ua);
    }

    public static void submitPDS(Graph graph, Node pdsNode) throws PMException {
        // deassign the PDS from the RBAC and PDS policy classes
        graph.deassign(pdsNode.getID(), getNodeID(graph, "RBAC_PDSs", OA, null));
        graph.deassign(pdsNode.getID(), getNodeID(graph, "PDSs", OA, null));

        // delete the associations for the PI
        long piID = getNodeID(graph, "PI", UA, null);
        graph.dissociate(piID, getNodeID(graph, "CoPI", OA, null));
        graph.dissociate(piID, getNodeID(graph, "ProjectInfo", OA, null));
        graph.dissociate(piID, getNodeID(graph, "BudgetInfo", OA, null));
        graph.dissociate(piID, getNodeID(graph, "PDSi", OA, null));
        graph.dissociate(piID, getNodeID(graph, "PI", OA, null));

        long copiID = getNodeID(graph, "CoPI", UA, null);
        graph.dissociate(copiID, getNodeID(graph, "SP", OA, null));
        graph.dissociate(copiID, getNodeID(graph, "ProjectInfo", OA, null));
        graph.dissociate(copiID, getNodeID(graph, "BudgetInfo", OA, null));

        long spID = getNodeID(graph, "SP", UA, null);
        graph.dissociate(spID, getNodeID(graph, "InvestigatorInfo", OA, null));
        graph.dissociate(spID, getNodeID(graph, "ProjectInfo", OA, null));
        graph.dissociate(spID, getNodeID(graph, "BudgetInfo", OA, null));

        // remove the pds from the submitted_pdss container, so no rbac permissions are given
        graph.deassign(pdsNode.getID(), getNodeID(graph, "submitted_pdss", OA, null));

        Node pdsiApprovalOA = graph.createNode(getID(), "PDSi approval", OA, null);
        graph.assign(pdsiApprovalOA.getID(), getNodeID(graph,"approval", OA, null));

        Node pendingOA = graph.createNode(getID(), "PDSi approval", OA, null);
        graph.assign(pendingOA.getID(), pdsiApprovalOA.getID());

        Node approvedOA = graph.createNode(getID(), "approved", OA, null);
        graph.assign(approvedOA.getID(), pdsiApprovalOA.getID());

        Node chairApprovalOA = graph.createNode(getID(), "chair approval", OA, null);
        graph.assign(chairApprovalOA.getID(), pendingOA.getID());

        Node deanApprovalOA = graph.createNode(getID(), "dean approval", OA, null);
        graph.assign(deanApprovalOA.getID(), pendingOA.getID());

        graph.assign(pdsNode.getID(), chairApprovalOA.getID());

        Node csChairApproval = graph.createNode(getID(), "cs_chair_approval", OA, null);
        graph.assign(csChairApproval.getID(), approvedOA.getID());

        Node mathChairApproval = graph.createNode(getID(), "math_chair_approval", OA, null);
        graph.assign(mathChairApproval.getID(), approvedOA.getID());

        Node coenDeanApproval = graph.createNode(getID(), "coen_dean_approval", OA, null);
        graph.assign(coenDeanApproval.getID(), approvedOA.getID());

        Node coasDeanApproval = graph.createNode(getID(), "coas_dean_approval", OA, null);
        graph.assign(coasDeanApproval.getID(), approvedOA.getID());

        // create the user attribute structure for approvals
        Node pdsiApprovalUA = graph.createNode(getID(), "PDSi approval", UA, null);
        graph.assign(pdsiApprovalUA.getID(), getNodeID(graph, "approval", UA, null));

        Node pdsiCommitteeUA = graph.createNode(getID(), "PDSi committee", UA, null);
        graph.assign(pdsiCommitteeUA.getID(), getNodeID(graph, "PDSi approval", UA, null));

        graph.associate(pdsiCommitteeUA.getID(), pdsiApprovalOA.getID(), new HashSet<>(Arrays.asList("read")));

        // assign all users of PI CoPI and SP to the PDSi committee
        Set<Long> users = graph.getChildren(getNodeID(graph, "PI", UA, null));
        for(Long id : users) {
            graph.assign(id, pdsiCommitteeUA.getID());
        }

        users = graph.getChildren(getNodeID(graph, "CoPI", UA, null));
        for(Long id : users) {
            graph.assign(id, pdsiCommitteeUA.getID());
        }

        users = graph.getChildren(getNodeID(graph, "SP", UA, null));
        for(Long id : users) {
            graph.assign(id, pdsiCommitteeUA.getID());
        }

        graph.associate(pdsiCommitteeUA.getID(), pdsiApprovalOA.getID(), new HashSet<>(Arrays.asList("read")));

        Node approvalCommitteeUA = graph.createNode(getID(), "approval committee", UA, null);
        graph.assign(approvalCommitteeUA.getID(), pdsiApprovalUA.getID());

        Node chairsUA = graph.createNode(getID(), "chairs", UA, null);
        graph.assign(chairsUA.getID(), getNodeID(graph, "approval committee", UA, null));

        Node deansUA = graph.createNode(getID(), "deans", UA, null);
        graph.assign(deansUA.getID(), getNodeID(graph, "approval committee", UA, null));

        graph.associate(chairsUA.getID(), chairApprovalOA.getID(), new HashSet<>(Arrays.asList("assign", "read")));
        graph.associate(deansUA.getID(), deanApprovalOA.getID(), new HashSet<>(Arrays.asList("assign", "read")));

        graph.associate(getNodeID(graph, "CS_Chair", UA, null), csChairApproval.getID(), new HashSet<>(Arrays.asList("assign to")));
        graph.associate(getNodeID(graph, "Math_Chair", UA, null), mathChairApproval.getID(), new HashSet<>(Arrays.asList("assign to")));
        graph.associate(getNodeID(graph, "COEN_Dean", UA, null), coenDeanApproval.getID(), new HashSet<>(Arrays.asList("assign to")));
        graph.associate(getNodeID(graph, "COAS_Dean", UA, null), coasDeanApproval.getID(), new HashSet<>(Arrays.asList("assign to")));

        graph.assign(getNodeID(graph, "CS_Chair", U, null), chairsUA.getID());
        graph.assign(getNodeID(graph, "Math_Chair", U, null), chairsUA.getID());

        graph.assign(getNodeID(graph, "COEN_Dean", U, null), deansUA.getID());
        graph.assign(getNodeID(graph, "COAS_Dean", U, null), deansUA.getID());
    }

    public static void chairApproval(Graph graph, Node pdsNode) throws PMException {
        // check that the PDS node is assigned to the cs chair approval and math chair approval container
        if(!graph.getParents(pdsNode.getID()).contains(getNodeID(graph, "cs_chair_approval", OA, null)) ||
                !graph.getParents(pdsNode.getID()).contains(getNodeID(graph, "math_chair_approval", OA, null))) {
            return;
        }

        // remove the PDS from the chair approval containers
        graph.deassign(pdsNode.getID(), getNodeID(graph, "chair approval", OA, null));
        graph.deassign(pdsNode.getID(), getNodeID(graph, "cs_chair_approval", OA, null));
        graph.deassign(pdsNode.getID(), getNodeID(graph, "math_chair_approval", OA, null));

        // assign the PDS to the dean approval container
        graph.assign(pdsNode.getID(), getNodeID(graph, "dean approval", OA, null));

        // grant the chairs read permissions on the PDS
        graph.associate(getNodeID(graph, "chairs", UA, null), pdsNode.getID(), new HashSet<>(Arrays.asList("read")));
    }

    public static void deanApproval(Graph graph, Node pdsNode) throws PMException {
        // check that the PDS node is assigned to the coen dean approval and coas dean approval conatiner
        if(!graph.getParents(pdsNode.getID()).contains(getNodeID(graph, "coas_dean_approval", OA, null)) ||
                !graph.getParents(pdsNode.getID()).contains(getNodeID(graph, "coen_dean_approval", OA, null))) {
            return;
        }

        // remove the PDS from the dean approval containers
        graph.deassign(pdsNode.getID(), getNodeID(graph, "dean approval", OA, null));
        graph.deassign(pdsNode.getID(), getNodeID(graph, "coen_dean_approval", OA, null));
        graph.deassign(pdsNode.getID(), getNodeID(graph, "coas_dean_approval", OA, null));

        // assign the PDS to the dean approval container
        graph.assign(pdsNode.getID(), getNodeID(graph, "PDSi approval", OA, null));

        // grant the deans read permissions on the PDS
        graph.associate(getNodeID(graph, "deans", UA, null), pdsNode.getID(), new HashSet<>(Arrays.asList("read")));
    }

}
