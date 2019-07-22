/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package segroute;
 
import java.util.Stack;

/**
 *
 * @author anix
 */
public class Packet {
    
    private int    id;
    private Node   src;
    private Node   dst;
    private Stack  labelStack;
    
    protected int hops;

    public Packet(int id, Node src, Node dst) {
        this.id  = id;
        this.src = src;
        this.dst = dst;
        this.hops = 0;
    }
    
    public Node getSrc() {
        return src;
    }

    public Node getDst() {
        return dst;
    }
    
    // source route the packet (add label stack)
    protected void srcRoute() {
        
    }
    
    protected void setLabelStack(Stack stack) {
       labelStack = stack;
    }
    
    protected void pushLabel(int label) {
        labelStack.push(label);
    }
    
    protected int getLabel() {
        if(labelStack.isEmpty()) {
            Node.warning("Packet::getLabel(): empty stack");
            return -1;
        }
        
        return((Integer) labelStack.peek());
    }
    
    protected int next() {
        if(labelStack.isEmpty()) {
            Node.warning("Packet::next(): empty stack");
            return -1;
        }
        
        return((Integer) labelStack.pop());
    }
    
    public String toString() {
        return ("PKT " + this.id + " Src: " + src.toString() + 
                "; Dst: " + dst.toString());
    }
    
}
