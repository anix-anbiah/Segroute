/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package segroute;

import java.util.List;
import java.util.ArrayList;
import eduni.simjava.Sim_system;
import static segroute.Network.warning;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

/**
 *
 * @author anix
 */
public class Controller {

    static private Controller controller = null;
    public static final int MAX_NETWORKS = 1;
    public static final int ROUNDS_PER_NETWORK = 200;
    static protected String OUTPUT_DIR = "";

    List<Node> nodes;
    List<Function> functions;

    private static PrintWriter writer;

    // Private constructor for singleton class Controller
    private Controller() {
        nodes = new ArrayList<>();
    }

    public Controller getInstance() {
        if (controller == null) {
            controller = new Controller();
        }

        return controller;
    }

    public void init() {
        // Create all the nodes and flow paths (from property files)- TBD
        // then, call execute() to start the controller
    }

    public static void main(String[] args) throws InterruptedException {

        String topo, netName, sfcStr;
        int ftTopoK, ftTopoH, numFlows, sfcLen;
        boolean ecmp;

        // Network[] network = new Network[MAX_NETWORKS];
        Network[] net = new Network[MAX_NETWORKS];
//        int totalRoundsTaken = 0;
//        int totalInitialVnfInstances = 0;
//        int totalFinalVnfInstances = 0;
        int netId = 0;

        // Initialise Sim_system
        Sim_system.initialise();

        netName = args[0];
        topo = args[1];
        ftTopoK = Integer.parseInt(args[2]);
        ftTopoH = Integer.parseInt(args[3]);

        numFlows = Integer.parseInt(args[5]);
        ecmp = Boolean.parseBoolean(args[6]);
        
        net[0] = new Network("net", topo, ftTopoK, ftTopoH, numFlows, ecmp);

    }

    private static void dumpln(String str) {
        writer.println(str);
    }
}
