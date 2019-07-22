/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package segroute;

import eduni.simjava.Sim_system;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.jgrapht.GraphPath;

/**
 *
 * @author anix
 */
public class RoRTopo {

    private Network net;
    private NetworkTest netTest;
    private Map<Integer, RingNode> ringNodes; // map of nodes indexed by node ID string
    private final Ring coreRing;
    private final int CORE_RING_ID = 1;

    public RoRTopo(Network net, NetworkTest netTest, int ringSize, int height) {
        this.net = net;
        this.netTest = netTest;
        ringNodes = new HashMap<>();

        coreRing = new Ring(ringSize, height, CORE_RING_ID, true, null);

        System.out.println("Number of entities in Sim_system = "
                + Sim_system.get_num_entities());

        System.out.println("Added " + net.nodes.size() + " nodes and "
                + net.graph.edgeSet().size() + " links ");

    }

    private class RingNode {

        Node node;
        Ring ring; // the ring to which the node belongs
        Ring childRing; // the next level down

        public RingNode(Node node, Ring ring) {
            this.node = node;
            this.ring = ring;
        }

        public Node getNode() {
            return node;
        }

        public Ring getRing() {
            return ring;
        }

        public Ring getChildRing() {
            return childRing;
        }

        public int getSrDomainId() {
            return node.getSrDomainId();
        }

    }

    protected void createInitialFlows() {

        int flowCount = 0;

        int numFlows = netTest.getNumFlows();

        for (int flowNum = 0; flowNum < numFlows; flowNum++) {

            ringCreateFlowBetweenRandomNodes(NetworkTest.DEFAULT_FLOW_RATE);

        }
    }

    private RingNode ringCreateNodeWithId(int nodeId, int nodeType, Ring ring, int srDomainId) {

        Node swtch;

        swtch = net.createNodeWithId(nodeId, nodeType, srDomainId);

        return new RingNode(swtch, ring);

    }

    private class Ring {

        private final int size; // number of nodes
        private final int height; // levels of sub-rings to create
        private final boolean core;
        private RingNode parentNode;

        // Hack alert
        private RingNode firstNode = null;
        private RingNode secondNode = null;

        List<RingNode> nodes;

        public Ring(int size, int height, int ringId, boolean core, RingNode parentNode) {
            this.size = size;
            this.height = height;
            this.core = core;
            this.parentNode = parentNode; // null for core ring!

            int nodesToCreate = size;
            int srDomainId;

            nodes = new ArrayList<>();

            // This is no longer true due to dual homing
//            if (core != null) {
//                // core node is part of this ring
//                nodes.add(core);
//                nodesToCreate--;
//            }
            RingNode node;
            int nodeId;

            for (int nd = 1; nd <= nodesToCreate; nd++) {

                // calculate the SR domain ID
                if (core) {
                    //default p=0
                    srDomainId = 0;
                    
                    // p = 1
//                    srDomainId = (nd <= size/2) ? 1:2; 
                    
                    // p = 2
//                    if(nd <= size/4) {
//                        srDomainId = 1;
//                    } else if (nd <= size/2) {
//                        srDomainId = 2;
//                    } else if (nd <= size *3/4) {
//                        srDomainId = 3;
//                    } else {
//                        srDomainId = 4;
//                    }
                    
                    // p = 3
                    if (nd <= size / 8) {
                        srDomainId = 1;
                    } else if (nd <= size / 4) {
                        srDomainId = 2;
                    } else if (nd <= size * 3 / 8) {
                        srDomainId = 3;
                    } else if (nd <= size / 2) {
                        srDomainId = 4;
                    } else if (nd <= size * 5/8) {
                        srDomainId = 5;
                    } else if (nd <= size * 3/4) {
                        srDomainId = 6;
                    } else if (nd <= size * 7/8) {
                        srDomainId = 7;
                    } else {
                        srDomainId = 8;
                    }
                    
                } else {
                    srDomainId = parentNode.getSrDomainId();
                }

                nodeId = ringId * 100 + nd;
                node = ringCreateNodeWithId(nodeId,
                        ((height == 1) ? Node.Type.OTHER : Node.Type.CORE),
                        this, srDomainId);

                nodes.add(node);
                ringNodes.put(nodeId, node);

                if (nd == 1) {
                    firstNode = node;
                }

                if (nd == 2) {
                    secondNode = node;
                }

//                if (height > 1 && (nd % 2 == 0)) {
                if (height > 1 && (nd % 2 == 0)
                        && (core || (nd != 2))) {
                    // create a child ring only IF
                    // (i) this is not a LEAF ring (last level)
                    // (ii) on an even node in the ring (create one child ring for 
                    //      each pair of nodes in the ring for dual homing)
                    // (iii) NOT on a node which is connected to parent ring
                    //       (first & second nodes connect to parent ring)
                    // node.childRing = new Ring(size, height - 1, nodeId, node);
                    node.childRing = new Ring(size, height - 1, nodeId, false, node);

                }

            }

            // now add the edges in this ring
            Iterator<RingNode> nodeItr = nodes.iterator();
            RingNode initNode = nodeItr.next();
            RingNode prevNode = initNode;
            RingNode nextNode;
            int ndId;
            Ring chRing;

            while (nodeItr.hasNext()) {
                nextNode = nodeItr.next();

                net.addEdge(prevNode.getNode(), nextNode.getNode());

                ndId = nextNode.getNode().getNodeId();

                chRing = nextNode.getChildRing();
                if (chRing != null) {
//                if (ndId % 2 == 0 && (height > 1)) {
                    // HACK - even node

                    net.addEdge(prevNode.getNode(), chRing.firstNode.getNode());
                    net.addEdge(nextNode.getNode(), chRing.secondNode.getNode());

// Adding the cross links
//                    net.addEdge(prevNode.getNode(), chRing.secondNode.getNode());
//                    net.addEdge(nextNode.getNode(), chRing.firstNode.getNode());
                }

                prevNode = nextNode;

            }

            net.addEdge(prevNode.getNode(), initNode.getNode());

        }

        public boolean isCoreRing() {
            return core;
        }

        public int getSize() {
            return size;
        }

        public int getHeight() {
            return height;
        }

    }

    // get a random ring node. Nodes on core ring and given excluded ring are excluded
    // from consideration
    private RingNode getRandomRingNode(Ring excludedRing, RingNode excludedNode) {
        Object[] allNodes;
        RingNode randomNode;
        int numNodes = ringNodes.size();
        int index;

        allNodes = (ringNodes.values().toArray());

        index = (int) (Math.random() * numNodes);
        randomNode = (RingNode) allNodes[index];

        while (randomNode.getRing().equals(excludedRing)
                || randomNode.equals(excludedNode)) {
            //    || randomNode.getRing().equals(coreRing)) {
            // System.out.println("Trying again");
            // try again
            index = (int) (Math.random() * numNodes);
            randomNode = (RingNode) allNodes[index];
        }

        return randomNode;
    }

    private RingNode getRandomRingNode() {
        return getRandomRingNode(null, null);
    }

    GraphPath route(RingNode srcNode, RingNode dstNode) {
        return null;
    }

    private void ringCreateFlowBetweenRandomNodes(int bitRate) {

        Flow flow;

        RingNode srcNode = getRandomRingNode(coreRing, null);

        //    boolean isRootPodAllowed = !srcNode.getPod().isRootPod();
        // RingNode dstNode = getRandomRingNode(srcNode.getRing());
        RingNode dstNode = getRandomRingNode(coreRing, srcNode);

        //    GraphPath path = route(srcNode, dstNode);
        flow = net.createFlowBetweenNodes(srcNode.getNode(), dstNode.getNode(), bitRate);

        Traffic.routePacket(flow);
    }

}
