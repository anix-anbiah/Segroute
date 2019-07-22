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
public class SrDomain {
    
    //Logical node IDs are calculated as offset + SR domain ID
    private static final int LNODE_OFFSET=0x10000000; 
    
    private int id; // SR Domain ID;
    private int numNodes;
    private int lnodeSID; // logical node SID

    public SrDomain(int id) {
        this.id = id;
        numNodes = 0;
        this.lnodeSID = LNODE_OFFSET + id;
    }
    
    public int getId() {
        return id;
    }
    
    protected void addNode(Node node) {
        numNodes++;
    }
    
    protected int getSize() {
        return numNodes;
    }
    
    protected int getLnodeId() {
        return lnodeSID;
    }
    
}
