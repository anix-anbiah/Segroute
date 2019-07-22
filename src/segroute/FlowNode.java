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
public class FlowNode {

    Flow flow;  // the flow of which this is a node

    Node node;      // the network node on which this flow node exists

    // ingress port for this flow path
    Port ingressPort;

    // egress port for this flow path
    Port egressPort;


    // indices (markers) indicating the segment of the SFC
    // hosted on this node. VNF instances between the prev and next markers
    // are on this node
    int previousIndex;
    int nextIndex;

    int dsDifferential; // downstream differential
    int dsMaxUtility;
    int dsDiffNodeId;   // Node ID if the source of the d/s differential
    boolean dsOfferPending;
    double dsOfferTimestamp; // time when offer was initiated

    public FlowNode(Node localNode, Flow flow, Port ingress, Port egress) {

        this.node = localNode;
        this.flow = flow;

        this.ingressPort = ingress;
        this.egressPort = egress;

    }

    public Flow getFlow() {
        return flow;
    }

    public int getPreviousIndex() {
        return previousIndex;
    }

    public int getNextIndex() {
        return nextIndex;
    }

    public void setPreviousIndex(int previousIndex) {
        this.previousIndex = previousIndex;
    }

    public void setNextIndex(int nextIndex) {
        this.nextIndex = nextIndex;
    }

}
