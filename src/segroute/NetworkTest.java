/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package segroute;

import eduni.simjava.Sim_system;
import static segroute.Network.info;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import org.jgrapht.*;

/**
 *
 * @author anix
 */
public class NetworkTest {

    private int unitTestId = 8;
    private int numLinks;
    private Map<Integer, FtNode> ftNodes; // map of nodes indexed by node ID string

    private Network net;
    private String topo; // topology on which to test
    private int topoArg1;
    private int topoArg2;  // parameters for the topology- for e.g. DCN has k & h
    private int numFlows; // number of flows to test with

    int vnfInstCount = 0;
    int vnfInstTor = 0;
    int vnfInstAggr = 0;
    int vnfInstCore = 0;
    int vnfInstOther = 0;

    protected static final int DEFAULT_FLOW_RATE = 100;

    public NetworkTest(Network net, String topo, int topoArg1, int topoArg2,
            int numFlows) {
        this.net = net;
        this.topo = topo;
        this.topoArg1 = topoArg1;
        this.topoArg2 = topoArg2;
        this.numFlows = numFlows;
    }

    public int getTopoArg1() {
        return topoArg1;
    }

    public int getTopoArg2() {
        return topoArg2;
    }

    public int getNumFlows() {
        return numFlows;
    }


    // unit test for DFP
    protected void addTopoAndInitialFlows() {

        if ("dcn".equals(topo)) {
            System.out.println("Testing network with DCN topology "
                    + "k = " + topoArg1 + "; h = " + topoArg2 + "; num flows = "
                    + numFlows);
            testDcnTopology(topoArg1, topoArg2);
            return;
        } else if ("ror".equals(topo)) {
            RoRTopo rorTopo = new RoRTopo(net, this, topoArg1, topoArg2);
            
            // topo has been added- create the SPTs
            net.createDsp();
            net.createKsp();
            
            rorTopo.createInitialFlows();
        } else if ("test".equals(topo)) {
            TestTopo tstTopo = new TestTopo(net, this);
            
            net.createDsp();
            net.createKsp();
            
            tstTopo.createFlows();
        }

        info("Network has been provisioned with topology and initial flows");
    }

    // DCN Topology nodes
    List<Node> coreSwitches, aggrSwitches, torSwitches, torSwitchesClone;

    private class FtNode {

        Node node;
        FtPod pod; // the pod in which this node is present

        public FtNode(Node node, FtPod pod) {
            this.node = node;
            this.pod = pod;
        }

        public Node getNode() {
            return node;
        }

        public FtPod getPod() {
            return pod;
        }

    }

    private FtNode ftCreateNodeWithId(int nodeId, int nodeType, FtPod pod) {

        Node swtch;

        swtch = net.createNodeWithId(nodeId, nodeType);

        return new FtNode(swtch, pod);

    }

    private GraphPath route(FtNode src, FtNode dst) {

        // assumption- at least one of the nodes must not be in the root pod
        FtPod srcPod = src.getPod();
        FtPod dstPod = dst.getPod();
        FtNode srcNode, dstNode;

        List<Node> srcPath;
        Stack<Node> dstPath;
        List<Link> links;

//        System.out.println("route: computing route from " + src.getNode().getNodeName()
//                + " and " + dst.getNode().getNodeName());
        if (srcPod.isRootPod() && dstPod.isRootPod()) {
            return null;
        }

        // assumption- src and dst have to be in different pods
        if (srcPod.equals(dstPod)) {
            return null;
        }

        srcPath = new ArrayList<>();
        dstPath = new Stack<>();
        links = new ArrayList<>();
        srcNode = src;
        dstNode = dst;

        // start from the lower among the heights of src & dst pod
        // and move your way up the FT
        int height = (srcPod.getH() > dstPod.getH()) ? dstPod.getH() : srcPod.getH();

        srcPath.add(srcNode.getNode());
        dstPath.push(dstNode.getNode());

        while (!srcPod.equals(dstPod)) {
            if (dstPod.getH() == height) {
                // go one level up from dst Pod
                dstPod = dstPod.parent;
                if (dstPod.equals(srcPod)) {
                    continue;
                }
                dstNode = dstPod.getRandomNode();
                dstPath.push(dstNode.getNode());
            }

            if (srcPod.getH() == height) {

                // go one level up from the src pod
                srcPod = srcPod.parent;
                if (srcPod.equals(dstPod)) {
                    continue;
                }
                srcNode = srcPod.getRandomNode();
                srcPath.add(srcNode.getNode());
            }

            height++;
        }
        // now, the path from src to dst is in srcPath and dstPath
        // pop the nodes from dstPath and add to srcPath
        while (!dstPath.isEmpty()) {
            srcPath.add(dstPath.pop());
        }

        // now src path is the full path from src to dst
        Iterator nodeItr = srcPath.iterator();
        Node currNode = null;
        Node nextNode = null;
        Link nextLink;

        while (nodeItr.hasNext()) {
            nextNode = (Node) nodeItr.next();
            if (currNode != null) {
                nextLink = net.graph.getEdge(currNode, nextNode);
//                System.out.print(nextLink.getName() + " ");
                links.add(nextLink);
            }
            currNode = nextNode;
        }

//        System.out.println(" End of Route");
        return new FtGraphPath(srcPath, links);
    }

    private class FtGraphPath<Node, Link> implements GraphPath {

        private List<Node> nodes;
        private List<Link> edges;

        public FtGraphPath(List nodes, List edges) {
            this.nodes = nodes;
            this.edges = edges;
        }

        public List<Node> getVertexList() {
            return nodes;
        }

        public List<Link> getEdgeList() {
            return edges;
        }

        @Override
        public Graph getGraph() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public Object getStartVertex() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public Object getEndVertex() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public double getWeight() {
            return 0.0;
        }

    }

    private class FtPod { // a pod in a fat tree topology

        private final int k; // part of a k-ary fat tree
        private final int h;
        FtPod parent;
        List<FtNode> switches;
        List<FtPod> children;

        public FtPod(int k, int h, int podNum) {
            this(k, h, podNum, null);
        }

//        public ftPod(int k, int h, int podNum, List<Node> coreSwitches) {
        public FtPod(int k, int h, int podNum, FtPod parent) {
            this.k = k;
            this.h = h;
            this.parent = parent;

            if (k % 2 != 0) {
                // k must be even
                System.out.println("Pod creator: k has to be even");
                return;
            }

//            info("Creating pod " + podNum + "; k= " + k + "; h = " + h);
            switches = new ArrayList<>();
            children = new ArrayList<>();

            FtNode swtch;
            int switchId;

            for (int sw = 1; sw <= k; sw++) {

                // create the Aggr switch
                switchId = podNum * 100 + sw;
                swtch = ftCreateNodeWithId(switchId, ((h == 1) ? Node.Type.TOR : Node.Type.AGGR), this);
                switches.add(swtch);
                ftNodes.put(switchId, swtch);

//                System.out.println("Created switch with id " + switchId + " at level " + h);
            }

            if (h == 1) { // we have reached the bottom of the tree
                return;
            }

            FtPod childPod;

            // else, create the children pods
            for (int sw = 1; sw <= k; sw++) {
                childPod = new FtPod(k / 2, h - 1, podNum * 100 + sw, this);
                children.add(childPod);
            }
        }

        public boolean isRootPod() {
            return (parent == null);
        }

        public int getK() {
            return k;
        }

        public int getH() {
            return h;
        }

        public void addEdges() {

            Iterator swItr, parentItr, childItr;
            FtNode swtch, parentSw;
            FtPod child;

            // every switch within the pod is connected to k parent switches
            swItr = switches.iterator();
            while ((parent != null) && swItr.hasNext()) {
                swtch = (FtNode) swItr.next();
//                boolean isEvenSw = (swtch.getNodeId() % 2 == 0);

                parentItr = parent.switches.iterator();
                while (parentItr.hasNext()) {
                    parentSw = (FtNode) parentItr.next();
//                    boolean isParentEvenSw = (parentSw.getNodeId() % 2 == 0);

//                    if (isEvenSw != isParentEvenSw) {
                    net.addEdge(parentSw.getNode(), swtch.getNode());
                    numLinks++;
//                    }
                }

            }

            childItr = children.iterator();
            while (childItr.hasNext()) {
                child = (FtPod) childItr.next();

                child.addEdges();
            }
        }

        public FtNode getRandomNode() {
            return switches.get((int) (Math.random() * k));
        }

        public FtNode getRandomLeafNode() {
            FtPod randomChild;

            if (h > 1) {
                randomChild = children.get((int) (Math.random() * k));

                return randomChild.getRandomLeafNode();
            }

            // else we have reached a leaf pod- return a random switch
            return switches.get((int) (Math.random() * k));
        }

    }

    protected FtNode getRandomFtNode() {
        return getRandomFtNode(null, true);
    }

    protected FtNode getRandomFtNode(FtNode excludedNode, boolean isRootPodAllowed) {
        Object[] allNodes;
        FtNode randomNode;
        int numNodes = ftNodes.size();
        int index;

        allNodes = (ftNodes.values().toArray());

        index = (int) (Math.random() * numNodes);
        randomNode = (FtNode) allNodes[index];

        while (randomNode.equals(excludedNode)
                || (randomNode.getPod().isRootPod() && !isRootPodAllowed)) {
            System.out.println("Trying again");
            // try again
            index = (int) (Math.random() * numNodes);
            randomNode = (FtNode) allNodes[index];
        }

        return randomNode;
    }

    private void ftCreateFlowBetweenRandomNodes(int bitRate) {

        FtNode srcNode = getRandomFtNode();

        boolean isRootPodAllowed = !srcNode.getPod().isRootPod();

        FtNode dstNode = getRandomFtNode(srcNode, isRootPodAllowed);

        GraphPath path = route(srcNode, dstNode);

        net.createFlowBetweenNodes(srcNode.getNode(), dstNode.getNode(), bitRate, path);

    }

    private void testDcnTopology(int k, int h) { // build a k-ary fat-tree topology of height h 

        Map<Integer, FtPod> pods;

        pods = new HashMap<>();
        ftNodes = new HashMap<>();

        FtPod pod;

        pod = new FtPod(k, h, 0);

        net.createDsp();
        net.createKsp();

        System.out.println("Number of entities in Sim_system = "
                + Sim_system.get_num_entities());

        numLinks = 0;

        pod.addEdges();

        System.out.println("Added " + net.nodes.size() + " nodes and "
                + numLinks + " links ");
        System.out.println("Testing for " + numFlows + " flows");

        int flowCount = 0;

        System.out.println("Adding " + numFlows + " INITIAL flows with SFC ");

        for (int flowNum = 0; flowNum < numFlows; flowNum++) {

            ftCreateFlowBetweenRandomNodes(DEFAULT_FLOW_RATE);

        }

        System.out.println("Done creating flows");
    }

    // code to add additional flows in the network. This is for
    // testing the dynamic case. 
    protected void addAdditionalFlows(int numAddnlFlows) {

        int flowCount = 0;

        for (int flowNum = 0; flowNum < numAddnlFlows; flowNum++) {
            ftCreateFlowBetweenRandomNodes(DEFAULT_FLOW_RATE);
        }

    }

}
