/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package segroute;

import java.util.List;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jgrapht.*;
import org.jgrapht.graph.*;

/**
 *
 * @author anix
 */
public class Flow {

    // the list of nodes that carry the flow
    List<Node> nodes;
    GraphPath<Node, Link> path;
    Stack labelStack;
    
    private Node src, dst;

    int bitRate;
    int flowId;

    private final static Logger logger = Logger.getLogger(Flow.class.getName());

    boolean debug;

    public Flow(Node src, Node dst, int bitRate, int flowId, GraphPath<Node, Link> path) {

        this.src = src;
        this.dst = dst;
        
        this.bitRate = bitRate;
        this.flowId = flowId;

        this.path = path;

        logger.setLevel(Network.defaultLogLevel);
        this.debug = false;
    }

    public GraphPath<Node, Link> getPath() {
        return path;
    }
    
    protected void setLabelStack(Stack stack) {
        this.labelStack = stack;
    }
    
    protected Stack getLabelStack() {
        return this.labelStack;
    }
    
    public int getBitRate() {
        return bitRate;
    }

    public int getFlowId() {
        return flowId;
    }

    public Node getSrc() {
        return src;
    }

    public Node getDst() {
        return dst;
    }
    
    public String toString() {
        return "F" + getFlowId();
    }

    public void setDebugFlag(boolean val) {
        this.debug = val;
    }

    public void info(String msg) {
        if (logger.isLoggable(Level.INFO) || debug) {
            System.out.println(msg);
        }
    }

    public void warning(String msg) {
        if (logger.isLoggable(Level.WARNING)) {
            System.out.println("WARNING: " + msg);
        }
    }
    
    public void prettyPrint() {
        FlowNode fnode;

        System.out.println(toString());
        System.out.println("PATH :: " + path.toString());

        System.out.print("FLOW NODES :: ");
        for (Node n : path.getVertexList()) {
            fnode = n.getFlowNodeByFlowId(flowId);

            System.out.print(fnode.node.toString() + " ");

            System.out.print("] ");
        }

        System.out.println();
    }

}
