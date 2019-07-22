/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package segroute;

/**
 *
 * @author anix
 */
public class TestTopo {

    private Network net;
    private NetworkTest netTest;

    private final int MAX_NODES = 5;
    private Node nodes[];

    public TestTopo(Network net, NetworkTest netTest) {
        this.net = net;
        this.netTest = netTest;

        createTopo(net);

        System.out.println("Added " + net.nodes.size() + " nodes and "
                + net.graph.edgeSet().size() + " links ");

    }

    private void createTopo(Network net) {

        // create a ring of 5 nodes
        int nodeId = 1;
        Node firstNode, lastNode = null, prevNode, node;

        firstNode = net.createNodeWithId(nodeId++, Node.Type.OTHER);
        prevNode = firstNode;
        while (nodeId <= MAX_NODES) {
            node = net.createNodeWithId(nodeId++, Node.Type.OTHER);
            net.addEdge(node, prevNode);
            prevNode = node;
            lastNode = node;
        }

        net.addEdge(firstNode, lastNode);
    }

    protected void createFlows() {
        
        Node src = net.getNodeByNodeId(1);
        Node dst = net.getNodeByNodeId(3);
        Flow flow = net.createFlowBetweenNodes(src, dst, NetworkTest.DEFAULT_FLOW_RATE);
        
        Traffic.routePacket(flow);
    }

}
