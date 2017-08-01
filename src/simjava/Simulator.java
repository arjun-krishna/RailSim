import models.*;

import eduni.simjava.*;
import eduni.simjava.distributions.*;
import java.util.*;

public class Simulator {
  public static void main(String[] args) {
    Globals globals = new Globals(args);
    Sim_system.initialise();

    // Create source for generating train packets according to schedule
    Source source = new Source("Source", globals);

    /*
    Create Stations
    station num , globals
    */
    for (String node : globals.adjL.keySet()) {
        if (node.charAt(0) == '$') {
            SignalNode sn = new SignalNode(node, globals);
        }
        else {
            Station st = new Station(node, globals);
            Sim_system.link_ports("Source", "out"+node, node, "in");
        }
    }

    int count = 0;
    int size = globals.adjL.keySet().size();
    for (String nodeX : globals.adjL.keySet()) {
        List<String> adjlist = (List<String>)globals.adjL.get(nodeX);

        // Link Creation Phase Printing
        System.err.println("linking progress : " + (count+1) + "/" +size);
        count++;

        for (String nodeY : adjlist) {
            Sim_system.link_ports(nodeX, "out"+nodeY, nodeY, "in");
        }
        globals.adjL.put(nodeX, null);
    }
    
    globals.adjL = null;
    Sim_system.run();
  }
}
