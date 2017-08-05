package models;
import eduni.simjava.*;
import eduni.simjava.distributions.*;
import java.util.*;

public class SignalNode extends Sim_entity {

  private String nodeName;
  private Sim_port in;
  private HashMap<String, Sim_port> out;
  private Globals globals;
  private HashMap<String, Double> timeRecord;
  private HashMap<String, Double> trainSpeed;
  private Set<String> trainsOccupying;

  // debug Log
  public void LOG(String x) {
    System.err.println("DEBUG--SIGNAL-LOG--*"+nodeName+"*~$ "+x);
  }

  // delay statistics log
  public void OUT(String train_id, Double delay) {
    System.out.println("DELAY_STAT--"+train_id+"--"+nodeName+"--"+delay+"--"+Sim_system.sim_clock());
  }

  // delay causing trains log
  public void TOUT(String train_id,List<SignalPacket> requesters ) {
    System.out.print("TRAIN_DELAYED--"+train_id+"--"+nodeName+"--");
    
    for (String trainId : trainsOccupying) {
      System.out.print(trainId+", ");
    }

    for (SignalPacket pkt : requesters) {
      System.out.print(pkt.train_id+", ");
    }
    System.out.println("");
  }

  // Constructor
  // param : name -> Nodes name
  //          g   -> Globals object for network details

  public SignalNode(String name, Globals g) {
    super(name);
    nodeName = name;
    globals = g;
    in = new Sim_port("in");
    add_port(in);
    out = new HashMap<String, Sim_port>();
    timeRecord = new HashMap<String, Double>();
    trainSpeed = new HashMap<String, Double>();
    trainsOccupying = new HashSet<String>();

    if (globals.adjL.containsKey(name)) {
      for (String node : globals.adjL.get(name)) {
        out.put(node, new Sim_port("out"+node));
        add_port(out.get(node));
      }
    }
  }

  // Application Logic running on the Signalling Node

  public void body() {

    // route object for the route of inbound train for forwarding
    List<String> route = new ArrayList<String>();
    List<SignalPacket> requesters = new ArrayList<SignalPacket>();   
                                                         // Queue of the
                                                         // access requesters

    while (Sim_system.running()) {

      Sim_event e = new Sim_event();
      sim_get_next(e);
      
      int eventType = e.get_tag();
      LOG("Signal Type : "+eventType);

      switch(eventType) {
        case 0 : {          // Train Arrival

          trainMovementInfo info = (trainMovementInfo) e.get_data();
          route = info.route;

          String train_id = info.train_id;
          

          // Send release lock signal to the previous node.
          if (!info.generatingNode.equals("Source"))
            sim_schedule(out.get(info.generatingNode), globals.signalDelay, 3, train_id);

          // Send RTS to the next node in the route.
          if (route.size() > 0) {
            Double time = Sim_system.sim_clock();
            timeRecord.put(train_id, time);
            trainSpeed.put(train_id, info.speed);
            trainsOccupying.add(train_id);
            String nextNode = route.get(0);
            SignalPacket sp = new SignalPacket(nodeName, train_id);
            sim_schedule(out.get(nextNode), globals.signalDelay, 1, sp);
          }

          sim_completed(e);
          break;
        }

        case 1 : {          // RTS [ Request To Send ]

          // Identify requester
          SignalPacket requestingNode = (SignalPacket) e.get_data();

          if (!globals.LOCK.available(nodeName)) {
            TOUT(requestingNode.train_id, requesters);
            // queue the requester in the nodes queue to process later
            requesters.add(requestingNode);

            // Send a dummy signal
            sim_schedule(out.get(requestingNode.nodeName), globals.signalDelay, 4);

          } else {
            // access has been granted
            globals.LOCK.acquire(nodeName);
            trainsOccupying.add(requestingNode.train_id);

            LOG("RTS from : "+requestingNode.nodeName);

            // Send CTS
            sim_schedule(out.get(requestingNode.nodeName), globals.signalDelay, 2, requestingNode);
          }

          sim_completed(e);
          break;
        }
        case 2 : {          // CTS [ Clear To Send ]

          SignalPacket msg = (SignalPacket) e.get_data();

          if (timeRecord.containsKey(msg.train_id)) {
            if (route.size() > 0) {              // Train has miles to go!
              String nextNode = route.remove(0);
              trainMovementInfo info = new trainMovementInfo(globals, route, nodeName, msg.train_id, globals.protocol, trainSpeed.get(msg.train_id));

              // Send the TRAIN to the next node
              Double delay = Sim_system.sim_clock() - timeRecord.get(msg.train_id);
              OUT(msg.train_id, delay);

              Double trainDelay = ((globals.linkDistance * 3600) / globals.speedHash.get(msg.train_id+nodeName) * 1.0); 

              if (globals.protocol == 3) {
                Sim_normal_obj normalDist = new Sim_normal_obj("normal", globals.mean, globals.var);
                Double sample = normalDist.sample();
                sample = Math.min(10, sample);
                sample = Math.max(-10, sample);
                trainDelay = Math.max(0 , ((globals.linkDistance * 3600) / (globals.speedHash.get(msg.train_id+nodeName) + sample ) * 1.0)) ;
                // if(msg.delayed == true){
                //   trainDelay = ((globals.linkDistance * 3600) / (globals.speedHash.get(msg.train_id+nodeName)*(1 - globals.p3Beta)) * 1.0);  
                // }
              }
              if (globals.protocol == 4) {
                
                Sim_uniform_obj unif = new Sim_uniform_obj("unif", 0, (globals.frame_size));
                double p = unif.sample();
                double threshold = 1.0 ;

                if (p <= threshold) {
                  double max_delay = ((globals.beta * globals.congestion) / Math.log(globals.N * globals.dilation));
                  Sim_uniform_obj uniDist = new Sim_uniform_obj("uniform", 1, max_delay);
                  trainDelay += uniDist.sample();
                }


              }

              sim_schedule(out.get(nextNode), trainDelay, 0, info);
              route = null;
            }
            timeRecord.remove(msg.train_id);
            trainSpeed.remove(msg.train_id);
          }
          sim_completed(e);
          break;
        }
        case 3 : {          // Release Lock Signal

          String train_id = (String) e.get_data();
          trainsOccupying.remove(train_id);

          if (requesters.isEmpty()) {
            globals.LOCK.release(nodeName);
          } else {
            // schedule a requester
            SignalPacket requestingNode = requesters.remove(0);
            requestingNode.delayed = true;
            sim_schedule(out.get(requestingNode.nodeName), globals.signalDelay, 2, requestingNode);
          }
          sim_completed(e);
          break;
        }
        case 4 : {          // Dummy Unused Signal
          sim_completed(e);
          break;
        }
        default : {
          sim_completed(e);
          break;
        }
      }
    }
  }

};
