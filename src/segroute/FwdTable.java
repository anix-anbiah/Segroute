/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package segroute;

import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

/**
 *
 * @author anix
 */
public class FwdTable {

    Map<Integer, List> table;
    private static Random rand = new Random();
    Node node;

    public FwdTable(Node node) {

        table = new HashMap<>();
        this.node = node;
    }

    protected int size() {
        return table.size();
    }

    protected void addEntry(int label, int action, Port egress) {

        FwdEntry entry;
        List<FwdEntry> entryList;

        entryList = (List) table.get(label);
        if (entryList == null) {
            // entry does not exist 
            entryList = new ArrayList<>();
            table.put(label, entryList);
        }

        entry = new FwdEntry(action, egress);
        if (entryList.contains(entry)) {
//            System.out.println("addEntry: entry exists already");
            return;
        }
        if (!entryList.isEmpty()) {
//            System.out.println(node.toString() + ": Adding multiple entries for " + label);
        }
        entryList.add(entry);

    }

    protected FwdEntry lookup(int label) {

        List entryList;
        int size;

        entryList = (List) table.get(label);
        size = entryList.size();

        if (size > 1) {
//            System.out.println(node.toString() + " lookup: Multiple entries for "
//                    + label + " (" + size + ")");
        }

        int rIndex = rand.nextInt(size);

        return (FwdEntry) entryList.get(rIndex);

    }
}
