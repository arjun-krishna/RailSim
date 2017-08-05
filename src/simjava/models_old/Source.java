package models;
import eduni.simjava.*;
import eduni.simjava.distributions.*;
import java.util.*;

public class Source extends Sim_entity {
  private HashMap<String,Sim_port> out;
  private Globals globals;

  // Constructor
  // param : name -> node's name
  //         g    -> Globals object for network topology

  public Source(String name, Globals g) {
    super(name);
    globals = g;
    out = new HashMap<String, Sim_port>();

    for (String node : globals.adjL.keySet()) {
      if (node.charAt(0) != '$') {
        out.put(node,new Sim_port("out"+node));
        add_port(out.get(node));
      }
    }
  }

  // Application Logic of the Source Node [The main scheduler]

  public void body() {

    for (double timestamp : globals.Schedule.keySet()) {
      List<String> route = globals.Schedule.get(timestamp);

      String train_id = globals.TrainIdentifier.get(timestamp);
      
      List<Integer> weeklyInfo = globals.weeklySchedule.get(train_id);
      for(int i=0; i<weeklyInfo.size(); i++){
        if(Integer.parseInt(""+weeklyInfo.get(i)) == 1){
          List<String> tempRoute = new ArrayList<String>();
          for(String x:route){
            tempRoute.add(x);
          }
          String startNode = tempRoute.get(0);
          tempRoute.remove(0);

          Double speed = 45.0;
          Double delay = 0.0;

          switch(globals.protocol) {
            case 0 : {
              speed = globals.speedHash.get(train_id + startNode).doubleValue();      // default speed
              delay = 0.0;                                 // Cause no delay in departure
              break;
            }
            case 1 : {
              double max_delay = ((globals.alpha * globals.congestion) / Math.log(globals.N * globals.dilation));
              Sim_uniform_obj uniDist = new Sim_uniform_obj("uniform", 1, max_delay);
              speed = globals.speedHash.get(train_id + startNode).doubleValue();      // default speed
              delay = uniDist.sample();
              break;
            }
            case 2 : {
              Sim_normal_obj normalDist = new Sim_normal_obj("normal", globals.mean, globals.var);
              speed = globals.speedHash.get(train_id + startNode).doubleValue() + (normalDist.sample());
              delay = 0.0;
              break;
            }
            case 3 : {
              Sim_normal_obj normalDist = new Sim_normal_obj("normal", globals.mean, globals.var);
              speed = globals.speedHash.get(train_id + startNode).doubleValue() + (normalDist.sample());
              delay = 0.0;
              break;
            }
            case 4 : {
              double max_delay = ((globals.beta * globals.congestion) / Math.log(globals.N * globals.dilation));
              Sim_uniform_obj uniDist = new Sim_uniform_obj("uniform", 1, max_delay);
              speed = globals.speedHash.get(train_id + startNode).doubleValue();     // default speed
              delay = uniDist.sample();
            }
          }

          trainMovementInfo info = new trainMovementInfo(globals, tempRoute, "Source", train_id,  globals.protocol, speed);
          sim_schedule(out.get(startNode), timestamp + delay + i*86400, 0, info);
        }
      }
      globals.Schedule.put(timestamp, null);
    }

    globals.Schedule = null;
    out = null;
  }
}
