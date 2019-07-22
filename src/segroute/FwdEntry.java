/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package segroute;

import java.util.Objects;

/**
 *
 * @author anix
 */
public class FwdEntry {
    
    private int action;
    private Port      port; // egress port
    
    public class FwdAction {
        public static final int CONTINUE = 0;
        public static final int NEXT = 1;
        public static final int PUSH = 2; // Not supported currently
    }

    public FwdEntry(int action, Port egress) {
        this.action = action;
        this.port   = egress;
    }

    public int getAction() {
        return action;
    }

    public Port getPort() {
        return port;
    }
    
    @Override
    public boolean equals(Object o) {
        FwdEntry fe = (FwdEntry) o;
        if((fe.getAction() == action) && fe.getPort().equals(port)) {
            return true;
        }
        
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 17 * hash + this.action;
        hash = 17 * hash + Objects.hashCode(this.port);
        return hash;
    }
    
}
