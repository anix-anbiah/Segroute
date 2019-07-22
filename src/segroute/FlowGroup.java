/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package segroute;

import java.util.List;
import java.util.ArrayList;
import java.util.Collection;

/**
 *
 * @author anix
 */
public class FlowGroup {
    
    List<Flow> flows;
    int        differential;
    int        maxUtility;
    int        totalBitRate; // aggregate bit rate of all flows
    
    
    public FlowGroup() {
        flows = new ArrayList<>();
        differential = 0;
    }

    public List<Flow> getFlows() {
        return flows;
    }

//    public void setFlows(List<Flow> flows) {
//        this.flows = flows;
//    }

    public int getDifferential() {
        return differential;
    }

    public void setDifferential(int differential) {
        this.differential = differential;
    }
    
    public void addFlow(Flow flow) {
        flows.add(flow);
    }
    
    public void addFlows(Collection toBeAdded) {
        flows.addAll(toBeAdded);
    }
    
    
}
