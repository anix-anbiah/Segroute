/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package segroute;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

import eduni.simjava.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author anix- Port class captures a port on a node
 */
public class Port {

    public static int HOST_PORT_ID = 1000; // reserve one port as source/sink for
    // all flows originating/terminating at this node

    public static int PORT_TYPE_HOST = 1;
    public static int PORT_TYPE_NET = 2;
    
    protected static final int ADJ_SID_OFFSET = 0x20000000;

    private final static Logger logger = Logger.getLogger(Port.class.getName());

    int portId; //port ID
    Node node; // local node
    Node neighbor; // neighbor connected to this port
    String linkName;
    Link link;
    int type;
    boolean debug;

    String portName;
    Sim_port simPort;

    List<Flow> ingressFlows;
    List<Flow> egressFlows;


    // Constructor for a NET type port
    public Port(Node node, Node neighbor, int portId, Link link) {

        this(node, portId);

        this.neighbor = neighbor;
        this.link = link;
        this.type = PORT_TYPE_NET;

        linkName = link.getName();
        portName = node.getNodeName() + "-" + linkName;

//        System.out.println("Adding port with name " + portName + " at node " + node.getNodeName());
        // naming convention of the port is "<nodeName>-<linkName>"
//        simPort = new Sim_port(portName);
//
//        node.add_port(simPort);
//        
//        if(node.get_port(portName) == null) {
//            System.out.println("Port " + portName + " not found on node " + node.getNodeName());
//        }
        
    }

    // constructor for a HOST type port
    public Port(Node node, int portId) {
        this.node = node;
        this.portId = portId;
        this.neighbor = null;
        this.link = null;
        this.type = PORT_TYPE_HOST;

        logger.setLevel(Network.defaultLogLevel);
        this.debug = false;

        this.ingressFlows = new ArrayList();
        this.egressFlows = new ArrayList();

        linkName = "HOST";
        portName = node.getNodeName() + "-" + linkName;

    }
    
    protected Node getNeighbor() {
        return neighbor;
    }

    public String getPortName() {
        return portName;
    }

    public Link getLink() {
        return link;
    }

    public int getPortId() {
        return portId;
    }
    
    public int getAdjSID() {
        return ADJ_SID_OFFSET + getPortId();
    }
    
    protected static boolean isAdjSID(int label) {
        if((label & ADJ_SID_OFFSET) != 0) {
            return true;
        }
        
        return false;
    }
    
    protected static int getPortIdFromAdjSID(int adjSID) {
        return adjSID % ADJ_SID_OFFSET;
    }

    public Sim_port getSimPort() {
        return simPort;
    }

    public void addFlow(Flow flow, boolean ingress) {

        if (ingress) {
            ingressFlows.add(flow);
        } else {
            egressFlows.add(flow);
        }
    }

    public void prettyPrint() {
        if (type == Port.PORT_TYPE_HOST) {
            System.out.print("HOST ");
        } else {
            System.out.print(neighbor.toString() + "[Port " + getPortId() + "]" + " ");
        }

    }


    public String toString() {
        if(node == null || neighbor == null) {
            return "Unknown Port";
        }
        return "Port " + node.toString() + "-" + neighbor.toString();
    }

    public void setDebugFlag(boolean val) {
        this.debug = val;
    }

    public void info(String msg) {
        if (logger.isLoggable(Level.INFO) || debug) {
            System.out.println(toString() + " " + msg);
        }
    }

    public void warning(String msg) {
        if (logger.isLoggable(Level.WARNING)) {
            System.out.println("WARNING: " + msg);
        }
    }

}
