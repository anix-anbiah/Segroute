/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package segroute;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Logger;
import eduni.simjava.Sim_entity;
import eduni.simjava.Sim_event;
import eduni.simjava.Sim_predicate;
import eduni.simjava.Sim_system;
import eduni.simjava.Sim_type_p;
import java.util.logging.Level;
import org.jgrapht.*;
import org.jgrapht.graph.*;
import org.jgrapht.alg.shortestpath.*;
import java.io.PrintWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.Stack;

/**
 *
 * @author anix
 */
public class Network {

    String name;
    String topo;
    int numFlows;
//    String outputDir;

    Map<Integer, Node> nodes; // map of nodes indexed by node ID string
    Map<Integer, SrDomain> srDomains;

    SimpleGraph<Node, Link> graph;
    LinkFactory linkFactory;
    DijkstraShortestPath<Node, Link> dsp;
    KShortestPaths<Node, Link> ksp;

    Map<Integer, Flow> flows;

    PrintWriter writer;

    double numRounds = 0.0;

    private final static Logger logger = Logger.getLogger(Network.class.getName());
    protected final static Level defaultLogLevel = Level.OFF;

    protected static final int DEFAULT_SR_DOMAIN = 0;

//    private final Sim_predicate triggerPred = new Sim_type_p(Message.Type.TRIGGER);
    public static int MAX_ROUNDS = 300;

    // interval (in rounds) between additional flow provisioning steps
    // this is applicable for the dynamic case
    public static final int ADDNL_FLOWS_INTERVAL = 50;

    // For the dynamic case, the interval between successive performance reports
    public static final int OPT_REPORT_INTERVAL = 10;

    // Flag to run dynamic provisioning of flows
    public static final int IS_DYNAMIC = 0;

    public static int KSHORTEST_K = 8;
    public static boolean KSHORTEST_ACTIVE = true;

    private NetworkTest networkTest;
    public int unitTestId = 0;
    public int deficitCost = 0;

    private int maxFwdTableSize;
    private double avgFwdTableSize;
    private int totalFwdEntries;

    protected int totalHops = 0;
    protected double avgHops;
    protected int maxHops = 0;

    protected int sfcLen;
    private boolean ecmp;

    int ftTopoK;
    int ftTopoH;

//    public Network(String netName, String topo, int ftTopoK, int ftTopoH,
//            String outputDir, int numFlows, int startRound, int endRound,
//            int sfcLen, boolean sfcHetero, boolean ecmp) {
    public Network(String netName, String topo, int ftTopoK, int ftTopoH,
            int numFlows, boolean ecmp) {

        this.name = netName;
        this.topo = topo;
        this.numFlows = numFlows;
        this.ecmp = ecmp;

        this.ftTopoK = ftTopoK;
        this.ftTopoH = ftTopoH;

        nodes = new HashMap<>();
        srDomains = new HashMap<>();
        flows = new HashMap<>();

        // create the default domain
        SrDomain defaultDomain = new SrDomain(DEFAULT_SR_DOMAIN);
        srDomains.put(DEFAULT_SR_DOMAIN, defaultDomain);

        linkFactory = new LinkFactory();

        graph = new SimpleGraph<>(linkFactory);

        logger.setLevel(defaultLogLevel);

        System.out.println("Created network " + netName);

        networkTest = new NetworkTest(this, topo, ftTopoK, ftTopoH, numFlows);
        networkTest.addTopoAndInitialFlows();

        prettyPrint();
    }

    public Map<Integer, Node> getNodes() {
        return nodes;
    }

    public SimpleGraph<Node, Link> getGraph() {
        return graph;
    }

    protected void createDsp() {
        dsp = new DijkstraShortestPath(graph);
    }

    protected void createKsp() {
        ksp = new KShortestPaths(graph, KSHORTEST_K);
    }

    private void dump(String str) {
        writer.print(str);
    }

    private void dumpln(String str) {
        writer.println(str);
    }

    // dump out the network- including nodes, flows and VNFs
    // in order to feed as input to the IP solver
    private void dumpInfo() {

        String nodeString;
        dumpln("# FT Topology K = " + ftTopoK + "; H = " + ftTopoH);
        dumpln("# Num of Flows = " + numFlows);
        dumpln("# Num of nodes = " + nodes.size() + "; Num of Links = " + graph.edgeSet().size());
        dumpln("");
        dumpln("");
        dumpln("data;"); // enter AMPL data mode
        dumpln("");

        dump("set NETWORK := " + name);
        dumpln(";");
        dumpln("");

        // next, dump out the nodes
        dump("set NODE :=");
        nodeString = "";
        for (Node n : nodes.values()) {
            nodeString += (" " + n.name);
        }

        dumpln(nodeString + ";");
        dumpln("");

        dump("set FLOW :=");
        for (Flow f : flows.values()) {
            dump(" " + f.toString());
        }
        dumpln(";");
        dumpln("");

        // Now dump the parameters
        // first, dump the network parameter
        dump("param net := ");
        dumpln(name + " 1;");
        dumpln("");

        // next, capacities of the nodes
        dump("param capacity :=");
        for (Node n : nodes.values()) {
            dump(" " + n.name + " " + n.capacity);
        }
        dumpln(";");
        dumpln("");

        dump("param path: " + nodeString + ":= ");
        List<Node> path;
        int index;

        for (Flow f : flows.values()) {
            path = f.getPath().getVertexList();

            dumpln("");

            dump(f.toString());

            for (Node n : nodes.values()) {
                index = path.indexOf(n);
                if (index == -1) {
                    dump(" 0");
                } else {
                    dump(" " + (index + 1));
                }
            }
        }
        dumpln(";");
        dumpln("");

        dumpln(";");
        dumpln("");

        // finally, dump the rate of all the flows
        dumpln("param rate " + ":= ");
        for (Flow f : flows.values()) {
            dump(" " + f.toString() + " " + f.getBitRate());
        }
        dumpln(";");
        dumpln("");
        dumpln("option solver gurobi;"); // Use Gurobi solver;
        dumpln("solve;"); // Instruct AMPL to solve this file
        dumpln("");
    }

//    protected Node createNodeWithId(int nodeId) {
//        this.createNodeWithId(nodeId, DEFAULT_SR_DOMAIN);
//    }
//
//    protected Node createNodeWithId(int nodeId, int srDomainId) {
//        String nodeName = name + "-N" + nodeId;
//
//        Node n = new Node(this, graph, nodeId, nodeName, 1000, srDomainId);
//        nodes.put(nodeId, n);
//        graph.addVertex(n);
//
//        return n;
//    }
    protected Node createNodeWithId(int nodeId, int nodeType) {
        return createNodeWithId(nodeId, nodeType, DEFAULT_SR_DOMAIN);
    }

    protected Node createNodeWithId(int nodeId, int nodeType, int srDomainId) {
        String nodeName = name + "-N" + nodeId;
        SrDomain domain;

        // see if the domain exists, if not, create it
        domain = srDomains.get(srDomainId);
        if (domain == null) {
            domain = new SrDomain(srDomainId);
            srDomains.put(srDomainId, domain);
        }

        Node n = new Node(this, graph, nodeId, nodeName, 1000, nodeType, srDomainId);
        nodes.put(nodeId, n);
        graph.addVertex(n);

        domain.addNode(n);

        return n;
    }

    protected Node createNode() {
        int nodeId = nodes.size() + 1;

        return createNodeWithId(nodeId, Node.Type.OTHER);
    }

    protected Link addEdge(Node n1, Node n2) {
//        System.out.println("Creating link from " + n1.toString() + " to " + n2.toString());
        return (Link) graph.addEdge(n1, n2);
    }

    protected Flow createFlowBetweenNodes(Node src, Node dst, int bitRate) {
        return createFlowBetweenNodes(src, dst, bitRate, null);
    }

    protected Flow createFlowBetweenNodes(Node src, Node dst, int bitRate, GraphPath path) {

        GraphPath<Node, Link> gp;
        List<GraphPath<Node, Link>> ecmpPaths = null;
        Stack labelStack = null;

        if (src.equals(dst)) {
            System.out.println("Cannot create flow when src,dst are equal");
            return null;
        }

        //    System.out.println("requesting path between " + src.getNodeName()
        //            + " and " + dst.getNodeName());
        if (!ecmp) {
            if (path == null) {
                gp = getPath(src, dst);
            } else {
                gp = path;
            }
        } else {
            ecmpPaths = getEcmpPaths(src, dst);
//            System.out.println("getEcmpPaths returned " + ecmpPaths.size() + " paths");
            gp = ecmpPaths.get(0);
        }

        int flowId = flows.size() + 1;
        //create the flow object
        Flow flow = new Flow(src, dst, bitRate, flows.size() + 1, gp);

        flows.put(flowId, flow);

        if (!ecmp) {
            // add the route (forwarding entries for this flow
            labelStack = addRoutes(flow, gp);
        } 
//        else {
//            ecmpPaths.stream().forEach((GraphPath<Node, Link> ecmpPath) -> {
////                System.out.println("Adding routes for ECMP path");
//                labelStack = addRoutes(flow, ecmpPath);
//            });
//        }

        flow.setLabelStack(labelStack);
        return flow;
    }

    private Stack addRoutes(Flow flow, GraphPath gp) {

        List<Link> pathLinks = gp.getEdgeList();
        List<Node> pathNodes = gp.getVertexList();

        // reverse the path links and nodes so that
        // we traverse the graph path in reverse
        // from destination to source
        Collections.reverse(pathLinks);
        Collections.reverse(pathNodes);

        // Now walk through the path and create the Fwd Entries.
        Iterator linkItr = pathLinks.iterator();
        Iterator nodeItr = pathNodes.iterator();

        Link prevl = null, nextl = null;
        Node n, prevn = null;
        Port egressPort;

        nextl = (Link) linkItr.next();

        // at the bottom of the label stack is the destination Node SID
        int label = flow.getDst().getNodeId();
        Stack labelStack = new Stack<>();
        labelStack.push(label);

        while (nodeItr.hasNext()) {

            n = (Node) nodeItr.next();

//            if (prevl == null) { // this is the first node
//                ingressPort = n.getHostPort();
//            } else {
//                ingressPort = prevl.getPortOnNode(n);
//            }
            if (prevl != null) { // this is NOT the dst node

                // check if the prevl spans sr domain boundaries
                if (prevn.getSrDomainId() != n.getSrDomainId()) {
                    info("addRoutes: Crossing SR Domain boundary at " + prevl.toString());
                    
                    // push the adj SID so that when forwarding
                    // using this label stack, this node will
                    // forward on this adjacency
                    label = prevl.getPortOnNode(n).getAdjSID();
                    labelStack.push(label);

                    // push the Node SID to the stack so that previous
                    // nodes on the path will route to this domain border node
                    label = n.getNodeId();
                    labelStack.push(label);
                } else {
                    // add a route for this flow on node
                    egressPort = prevl.getPortOnNode(n);
                    n.addRoute(label, egressPort);
                }

            }

//            n.addFlowNode(flow, ingressPort, egressPort);
//            info("createFlow: Processing " + prevl.toString() + " " + nextl.toString());
            prevl = nextl;
            prevn = n;

            if (linkItr.hasNext()) {
                nextl = (Link) linkItr.next();
            } else {
                nextl = null;
            }

        }
        
        return labelStack;
    }

    private List getEcmpPaths(Node src, Node dst) {

        List<GraphPath<Node, Link>> kshortestpaths;
        GraphPath<Node, Link> shortestPath, nextPath;
        int shortestDistance, numShortestPaths;

        if (src.equals(dst)) {
            return null;
        }

        if (!ecmp) {
            return null;
        }

        // KSHORTEST PATH is ACTIVE
        kshortestpaths = ksp.getPaths(src, dst);

        shortestPath = kshortestpaths.get(0);
        shortestDistance = shortestPath.getLength();
        numShortestPaths = 1;

        for (int i = 1; i < kshortestpaths.size(); i++) {
            nextPath = kshortestpaths.get(i);
            if (nextPath.getLength() > shortestDistance) {
                break;
            }
            numShortestPaths += 1;
        }

        return kshortestpaths.subList(0, numShortestPaths);
    }

    private GraphPath getPath(Node src, Node dst) {

        List<GraphPath<Node, Link>> kshortestpaths;
        int randomIndex;

        if (src.equals(dst)) {
            return null;
        }

        if (!ecmp) {
            return dsp.getPath(src, dst);
        }

        // KSHORTEST PATH is ACTIVE
        kshortestpaths = getEcmpPaths(src, dst);

        // if there are multiple shortest paths- randomly pick one
        randomIndex = (int) (Math.random() * kshortestpaths.size());

        return kshortestpaths.get(randomIndex);
    }

    protected SrDomain getSrDomainById(int srDomainId) {
        return (SrDomain) srDomains.get(srDomainId);
    }

    protected Node getNodeByNodeId(int nodeId) {
        return (Node) nodes.get(nodeId);
    }

    protected Node getRandomNode() {
        return getRandomNode(null);
    }

    protected Node getRandomNode(Node excludedNode) {
        Object[] allNodes;
        Node randomNode;
        int numNodes = nodes.size();
        int index;

        allNodes = (nodes.values().toArray());

        index = (int) (Math.random() * numNodes);
        randomNode = (Node) allNodes[index];

        while (randomNode.equals(excludedNode)) {
            // try again
            index = (int) (Math.random() * numNodes);
            randomNode = (Node) allNodes[index];
        }

        return randomNode;
    }

    protected Flow getFlowByFlowId(int flowId) {
        return (Flow) flows.get(flowId);
    }

    public Logger getLogger() {
        return logger;
    }

    public static void info(String msg) {
        if (logger.isLoggable(Level.INFO)) {
            System.out.println(msg);
        }
    }

    public static void warning(String msg) {
        if (logger.isLoggable(Level.WARNING)) {
            System.out.println("WARNING: " + msg);
        }
    }

    public static double clock() {
        return Sim_system.clock();
    }

    private void updateFwdEntryCount() {

        maxFwdTableSize = 0;
        avgFwdTableSize = 0.0;
        totalFwdEntries = 0;

        int fwdTableSize;

        for (Node n : nodes.values()) {
            fwdTableSize = n.getFwdTableSize();
            if (fwdTableSize > maxFwdTableSize) {
                maxFwdTableSize = fwdTableSize;
            }
            totalFwdEntries += fwdTableSize;
        }

        avgFwdTableSize = (totalFwdEntries * 100 / nodes.size()) / 100.0;

        avgHops = (totalHops * 100 / flows.size()) / 100.0;
    }

    public final void prettyPrint() {

        updateFwdEntryCount();

        System.out.println("Topo " + topo + "(" + ftTopoK + "," + ftTopoH + "); "
                + "ECMP " + (ecmp ? "Enabled" : "Disabled"));
        System.out.print("SR Domains ");

        srDomains.values().stream().forEach((domain) -> {
            System.out.print(" Domain " + domain.getId() + " (" + domain.getSize() + " nodes); ");
        });

        System.out.println("");

        System.out.println("Number of flows      = " + flows.size());

        System.out.println("Avg Hops = " + avgHops);
        System.out.println("Max Hops = " + maxHops);

        System.out.println("Avg Fwd Table Size = " + avgFwdTableSize);
        System.out.println("Max Fwd Table Size = " + maxFwdTableSize);
        System.out.println("Total Fwd Entries  = " + totalFwdEntries);

        System.out.println("Max Label Stack Size = " + Traffic.getMaxStackSize());
    }

    // Methods that return the STATS required for the graph plot
    protected int roundsTaken() {
        return 0;
    }

    protected int initialVnfInstances() {
        return 0;
    }

    protected int finalVnfInstances() {
        return 0;
    }
}
