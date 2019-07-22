/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package segroute;

import eduni.simjava.Sim_system;
import java.util.logging.Logger;
import org.jgrapht.*;
import org.jgrapht.graph.*;

/**
 *
 * @author anix- represents an undirected link in the network
 */
public class Link {

    Network net;
    Node from, to;
    Port fromPort, toPort;

    SimpleGraph<Node, Link> graph;

    String name;
        

    public Link(Node from, Node to) {

        super();
        
//        this.net = Network.getInstance();

        if (from.getNodeName().compareTo(to.getNodeName()) < 0) {
            this.name = "L-" + from.getNodeName() + "-" + to.getNodeName();
        } else {
            this.name = "L-" + to.getNodeName() + "-" + from.getNodeName();
        }

//        info("Creating link " + this.name);
        this.from = from;
        this.to  = to;
        fromPort = from.addNetworkLink(to, this);
        toPort   = to.addNetworkLink(from, this);
        
//        System.out.println("Linking ports " + from.getNodeName() + ":" + fromPort.getPortName() +
//                " and " + to.getNodeName() + ":" + toPort.getPortName());
//        
//        System.out.println(Sim_system.running()? "RUNNING" : "NOT RUNNING");
       
//        Sim_system.link_ports(from.getNodeName(), fromPort.getPortName(),
//                to.getNodeName(), toPort.getPortName());

    }

    public Port getFromPort() {
        return fromPort;
    }

    public Port getToPort() {
        return toPort;
    }

    public Node getFrom() {
        return from;
    }

    public Port getPortOnNode(Node node) {
        assert (node.equals(getFrom()) || node.equals(getTo()));

        if (node.equals(getFrom())) {
            return fromPort;
        } else {
            return toPort;
        }
    }

    public Node getTo() {
        return to;
    }

    public String getName() {
        return name;
    }

    public String toString() {
        return name;
    }

    protected Node getSource() {
        return from;
    }

    protected Node getTarget() {
        return to;
    }
    
    protected void setDebugAtPort(Node n) {
        Port p = getPortOnNode(n);
        p.setDebugFlag(true);
    }

    private void info(String msg) {
        Network.info(msg);
    }

}
