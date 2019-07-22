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
public class Traffic {
    
    static int pktId = 1;
    static int maxStackSize = 0;
    
    protected static void routePacket(Flow flow) {
        
        Stack labelStack;
        
        Network.info("Routing a packet from " + flow.getSrc().toString() +
                " to " + flow.getDst().toString());
        
        Packet pkt = new Packet(pktId++, flow.getSrc(), flow.getDst());
        labelStack = flow.getLabelStack();
        pkt.setLabelStack(labelStack);
        
        int stackSize = labelStack.size();
        if(stackSize > maxStackSize) {
            maxStackSize = stackSize;
        }
        
        flow.getSrc().routePacket(pkt);
    }
    
    protected static int getMaxStackSize() {
        return maxStackSize;
    }
    
}
