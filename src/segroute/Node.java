/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package segroute;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Comparator;
import java.util.Map;
import java.util.HashMap;
import java.util.Collection;
import eduni.simjava.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jgrapht.*;
import org.jgrapht.graph.*;
import segroute.FwdEntry.FwdAction;

/**
 *
 * @author anix
 */
public class Node {

    int nodeId; // ID of the node

    static private final int DEFAULT_CAPACITY = 100;

    static protected final int PKT_MAX_HOPS = 50;

    private final static Logger logger = Logger.getLogger(Node.class.getName());

    Controller controller;
    Network net;
    SimpleGraph<Node, Link> graph;
    FwdTable fwdTable;
    SrDomain srDomain;

    public static class Type {

        static final int OTHER = 0;
        static final int TOR = 1;
        static final int AGGR = 2;
        static final int CORE = 3;
        static final int HYPER = 4;
    };

    public static final String NODE_TYPE[] = {"OTHER", "TOR", "AGGR", "CORE"};

    String name; // name of the node
    int totalLoad;
    int capacity;
    int type;

    Map<Integer, Flow> flows; // a map of flows indexed by flow ID
    Map<Integer, FlowNode> flowNodes; // a map of flow nodes indexed by flow ID

    Map<Integer, Port> neighborPorts; // ports indexed by neighbor node ID
    Map<Integer, Port> ports; // ports indexed by port ID
    Map<Integer, Node> neighbors; // neighbors indexed by node ID

    // A holder for all outgoing messages to be sent out
    // at the end of each round
    List<FlowNode> paths;

    final double LOAD_THRESHOLD_FACTOR = 0.7;

    boolean debug;

    public Map<Integer, Port> getPorts() {
        return ports;
    }

    public Node(Network net, SimpleGraph<Node, Link> graph,
            int nodeId, String name, int capacity, int type) {
        this(net, graph, nodeId, name, capacity, type, Network.DEFAULT_SR_DOMAIN);
    }

    public Node(Network net, SimpleGraph<Node, Link> graph,
            int nodeId, String name, int capacity, int type, int srDomainId) {

        this.net = net;
        this.graph = graph;
        this.nodeId = nodeId;
        this.name = name;
        this.type = type;
        this.srDomain = net.getSrDomainById(srDomainId);

        this.fwdTable = new FwdTable(this);

        logger.setLevel(Network.defaultLogLevel);
        this.debug = false;

        this.capacity = capacity;
        this.paths = new ArrayList<>();

        this.ports = new HashMap<>();
        this.flowNodes = new HashMap<>();

        // create the HOST port for this node
        Port hostPort = new Port(this, Port.HOST_PORT_ID);
        this.ports.put(Port.HOST_PORT_ID, hostPort);

    }

    public Node(Network net, SimpleGraph<Node, Link> graph,
            int nodeId, String name, int capacity) {
        this(net, graph, nodeId, name, capacity, Type.OTHER);
    }

    public int getNodeId() {
        return nodeId;
    }

    public int getSrDomainId() {
        return srDomain.getId();
    }

    protected SrDomain getSrDomain() {
        return srDomain;
    }

    public SimpleGraph<Node, Link> getGraph() {
        return graph;
    }

    public String getNodeName() {
        return name;
    }

    public String toString() {
        return name;
    }

//    public void body() {
//
//        double round;
//
//        while (Sim_system.running()) {
//
////            triggerCount = sim_waiting(triggerPred);
////
////            if (triggerCount == 0) {
////                // wait till you receive a trigger
////            }
////            offerCount = sim_waiting(offerPred);
////            info("Trigger received by Node " + toString() + "; Trigger Count "
////                    + triggerCount);
//            Sim_event e = new Sim_event();
//
//            round = Network.clock();
//
//            info("Trigger event received at time " + round);
//
//            info("Processing a round at node " + getNodeName()
//                    + " at time " + e.event_time());
//
//            // Process the trigger event
//            sim_process(0.0);
//
//            // The event has been serviced
//            sim_completed(e);
//
//        }
//    }
    private boolean isPastEndRound() {
        return false;

    }

    public Port addNetworkLink(Node neighbor, Link link) {
        // get the next available port ID
        int portId = ports.values().size();

        Port port = new Port(this, neighbor, portId, link);
        ports.put(portId, port);

        return port;

    }

    protected Port getPortById(int portId) {
        return ports.get(portId);
    }

    protected void addFwdEntry(int label, int action, Port egress) {
        fwdTable.addEntry(label, action, egress);
    }

    protected void addRoute(int label, Port egress) {
        addFwdEntry(label, FwdAction.CONTINUE, egress);
    }

    protected void routePacket(Packet pkt) {

        int label;
        Port port;
        Node nextNode;
        FwdEntry fwdEntry;

        if (this.equals(pkt.getDst())) {

            net.totalHops += pkt.hops;
            if (net.maxHops < pkt.hops) {
                net.maxHops = pkt.hops;
            }

            // we are done- SEGR TBD- update some stats
            return;
        }

        if (pkt.hops > PKT_MAX_HOPS) {
            warning("Packet " + pkt.toString() + " reached MAX HOPS");
            return;
        }

        label = pkt.getLabel();

        // first check if the label is an Adj SID
        if (Port.isAdjSID(label)) {
            info("Adj SID in label stack at " + toString());
            port = getPortById(Port.getPortIdFromAdjSID(label));
            pkt.next(); // pop the Adj SID 
            nextNode = port.getNeighbor();
        } else {

            fwdEntry = fwdTable.lookup(label);
            if (fwdEntry == null) {
                info("Unable to find fwd entry for Packet " + pkt.toString());
                return;
            }

//        if (fwdEntry.getAction() == FwdAction.NEXT) {
//            pkt.next(); // advance to the next segment of the source route
//        }
            port = fwdEntry.getPort();
            nextNode = port.getNeighbor();
        }

        if (label == nextNode.getNodeId()) {
            // penultimate node pop
            info("Penultimate node label pop at " + toString());
            pkt.next();
        }

        pkt.hops++;
        nextNode.routePacket(pkt);

        return;

    }

    public Port getHostPort() {
        return (Port) ports.get(Port.HOST_PORT_ID);
    }

    public FlowNode getFlowNodeByFlowId(int flowId) {
        return (FlowNode) flowNodes.get(flowId);
    }

    public void addFlowNode(Flow flow, Port ingress, Port egress) {

        FlowNode fnode = new FlowNode(this, flow, ingress, egress);
        flowNodes.put(flow.getFlowId(), fnode);

//        info("Adding Flow Node Flow ID " + flow.getFlowId() + " at Node " + toString() + "; Ingress "
//                + ingress.getPortId() + "; Egress " + egress.getPortId());
        // add the flow to the ingress and egress ports
        ingress.addFlow(flow, true);
        egress.addFlow(flow, false);

    }

    public void setDebugFlag(boolean val) {
        this.debug = val;
    }

    public void info(String msg) {
        if (logger.isLoggable(Level.INFO) || debug) {
            System.out.println(toString() + " " + msg);
        }
    }

    public static void warning(String msg) {
        if (logger.isLoggable(Level.WARNING)) {
            System.out.println("WARNING: " + msg);
        }
    }

    public void prettyPrint() {
        System.out.print("Node: " + name + " " + " ");
        System.out.print("Neighbors :: ");
        for (Port p : ports.values()) {
            p.prettyPrint();
        }

        System.out.println();
    }

    protected int getFwdTableSize() {
        return fwdTable.size();
    }

}
