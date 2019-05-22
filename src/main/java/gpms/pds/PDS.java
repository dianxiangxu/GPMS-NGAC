package pds;

import gov.nist.csd.pm.decider.PReviewDecider;
import gov.nist.csd.pm.exceptions.PMException;
import gov.nist.csd.pm.graph.Graph;
import gov.nist.csd.pm.graph.GraphSerializer;
import gov.nist.csd.pm.graph.MemGraph;
import gov.nist.csd.pm.graph.model.nodes.Node;
import gov.nist.csd.pm.graph.model.nodes.NodeType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static gov.nist.csd.pm.graph.model.nodes.NodeType.O;
import static gov.nist.csd.pm.graph.model.nodes.NodeType.OA;
import static gov.nist.csd.pm.graph.model.nodes.NodeType.U;

public class PDS {

    public static Random rand = new Random();

    public static void main(String[] args) throws IOException, PMException {
        // load the initial configuration from json
        String json = new String(Files.readAllBytes(Paths.get("docs/pds.pm")));
        Graph graph = GraphSerializer.fromJson(new MemGraph(), json);

        printAccessState("Initial configuration", graph);

        // Step 1. Bob creates a PDS and assigns it to RBAC_PDSs
        Node pdsNode = graph.createNode(getID(), "PDSi", OA, null);
        long rbacPDSsID = getNodeID(graph, "RBAC_PDSs", OA, null);
        graph.assign(pdsNode.getID(), rbacPDSsID);
        // simulate an event
        // normally the Event Processing Point will do this, so we'll just simulate it by
        // calling the simulateAssignToEvent method
        long bobID = getNodeID(graph, "bob", U, null);
        simulateAssignToEvent(graph, bobID, graph.getNode(rbacPDSsID), pdsNode);

        printAccessState("After bob creates PDS", graph);

        // Step 2. Bob adds alice as a CoPI
        Node aliceObj = graph.createNode(getID(), "alice", O, null);
        long copiID = getNodeID(graph, "CoPI", OA, null);
        graph.assign(aliceObj.getID(), copiID);
        // simulate an event
        simulateAssignToEvent(graph, bobID, graph.getNode(copiID), aliceObj);

        printAccessState("After bob adds alice as a CoPI", graph);

        // Step 3. Alice adds Charlie as a CoPI
        Node charlieObj = graph.createNode(getID(), "charlie", O, null);
        long spID = getNodeID(graph, "SP", OA, null);
        graph.assign(charlieObj.getID(), spID);
        // simulate an event
        long aliceID = getNodeID(graph, "alice", U, null);
        simulateAssignToEvent(graph, aliceID, graph.getNode(spID), charlieObj);

        printAccessState("After alice adds charlie as a SP", graph);

        // Step 4. Bob submits the PDS for approval
        long submittedPDSs = getNodeID(graph, "submitted_pdss", OA, null);
        graph.assign(pdsNode.getID(), submittedPDSs);
        // simulate an event
        simulateAssignToEvent(graph, bobID, graph.getNode(submittedPDSs), pdsNode);

        printAccessState("After bob submits the PDS for approval", graph);

        // Step 5. CS Chair approves
        long csChairApproval = getNodeID(graph, "cs_chair_approval", OA, null);
        graph.assign(pdsNode.getID(), csChairApproval);
        // simulate an event
        simulateAssignToEvent(graph, getNodeID(graph, "CS_Chair", U, null), graph.getNode(csChairApproval), pdsNode);

        printAccessState("After the CS Chair approves the PDS", graph);

        // Step 6. Math Chair approves
        long mathChairApproval = getNodeID(graph, "math_chair_approval", OA, null);
        graph.assign(pdsNode.getID(), mathChairApproval);
        // simulate an event
        simulateAssignToEvent(graph, getNodeID(graph, "Math_Chair", U, null), graph.getNode(mathChairApproval), pdsNode);

        printAccessState("After Math Chair approves the PDS", graph);

        // Step 7. COEN Dean approves
        long coenDeanApproval = getNodeID(graph, "coen_dean_approval", OA, null);
        graph.assign(pdsNode.getID(), coenDeanApproval);
        // simulate an event
        simulateAssignToEvent(graph, getNodeID(graph, "COEN_Dean", U, null), graph.getNode(coenDeanApproval), pdsNode);

        printAccessState("After the COEN Dean approves the PDS", graph);

        // Step 8. COAS Dean approves
        long coasDeanApproval = getNodeID(graph, "coas_dean_approval", OA, null);
        graph.assign(pdsNode.getID(), coasDeanApproval);
        // simulate an event
        simulateAssignToEvent(graph, getNodeID(graph, "COAS_Dean", U, null), graph.getNode(coasDeanApproval), pdsNode);

        printAccessState("After the COAS Dean approves the PDS", graph);
    }

    /**
     * Utility method to print the current access state to the console.
     * @param step the name of the step
     * @param graph the graph to determine permissions
     */
    private static void printAccessState(String step, Graph graph) throws PMException {
        System.out.println("############### Access state for " + step + " ###############");

        // initialize a PReviewDecider to make decisions
        PReviewDecider decider = new PReviewDecider(graph);

        // get all of the users in the graph
        Set<Node> search = graph.search(null, U.toString(), null);
        for(Node user : search) {
            // there is a super user that we'll ignore
            /*if(user.getName().equals("super")) {
                continue;
            }*/

            System.out.println(user.getName());
            // get all of the nodes accessible for the current user
            Map<Long, Set<String>> accessibleNodes = decider.getAccessibleNodes(user.getID());
            for(long objectID : accessibleNodes.keySet()) {
                Node obj = graph.getNode(objectID);
                System.out.println("\t" + obj.getName() + " -> " + accessibleNodes.get(objectID));
            }
        }
        System.out.println("############### End Access state for " + step + "############");
    }

    /**
     * Method to simulate an obligation. All obligations used in this example are triggered by an "assign to" event,
     * so we'll assume there is a child node being assigned to a target node.
     * @param graph the graph
     * @param userID the ID of the user that triggered the event
     * @param targetNode the node that the event happens on
     * @throws PMException
     */
    private static void simulateAssignToEvent(Graph graph, long userID, Node targetNode, Node childNode) throws PMException {
        // check if the target of the event is a particular container and execute the corresponding "response"
        if(targetNode.getID() == getNodeID(graph, "RBAC_PDSs", OA, null)) {
            Obligations.createPDS(graph, userID, childNode);
        } else if(targetNode.getID() == getNodeID(graph, "CoPI", OA, null)) {
            Obligations.addCoPI(graph, childNode);
        } else if(targetNode.getID() == getNodeID(graph, "SP", OA, null)) {
            Obligations.addSP(graph, childNode);
        } else if(targetNode.getID() == getNodeID(graph, "submitted_pdss", OA, null)) {
            Obligations.submitPDS(graph, childNode);
        } else if(targetNode.getID() == getNodeID(graph, "cs_chair_approval", OA, null) ||
                targetNode.getID() == getNodeID(graph, "math_chair_approval", OA, null)) {
            Obligations.chairApproval(graph, childNode);
        }  else if(targetNode.getID() == getNodeID(graph, "coen_dean_approval", OA, null) ||
                targetNode.getID() == getNodeID(graph, "coas_dean_approval", OA, null)) {
            Obligations.deanApproval(graph, childNode);
        }
    }

    /**
     * Utility function to get the ID of a node iven it's name, type, and any properties it may have.
     */
    public static long getNodeID(Graph graph, String name, NodeType type, Map<String, String> properties) throws PMException {
        Set<Node> search = graph.search(name, type.toString(), properties);
        if(search.isEmpty()) {
            throw new PMException("no node with name " + name + ", type " + type + ", and properties " + properties);
        }

        return search.iterator().next().getID();
    }

    public static long getID() {
        return rand.nextLong();
    }
}
