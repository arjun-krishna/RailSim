package models;
import eduni.simjava.*;
import eduni.simjava.distributions.*;
import java.util.*;

public class Station extends Sim_entity {
  private Sim_port in;
  private HashMap<String, Sim_port> out;
  private Globals globals;
  private Integer platforms;
  private String nodeName;
  private HashMap<String, Double> timeRecord;
  private HashMap<String, Double> trainSpeed;
  private HashMap<String, List<String> > trainRecord;
  private Set<String> trainsOccupying;

  // debug log
  public void LOG(String x) {
    System.err.println("DEBUG--STATION-LOG--*"+nodeName+"*~$ "+x);
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
  // params  :    name -> node's name
  //              g    -> Globals object for Network topology
  //              p    -> platform count

  public Station(String name, Globals g, Integer p) {
    super(name);
    nodeName = name;
    globals = g;
    platforms = p;
    in = new Sim_port("in");
    add_port(in);
    out = new HashMap<String, Sim_port>();
    timeRecord = new HashMap<String, Double>();
    trainSpeed = new HashMap<String, Double>();
    trainRecord = new HashMap<String, List<String> >();
    trainsOccupying = new HashSet<String>();

    if (globals.adjL.containsKey(name)) {
      for (String node : globals.adjL.get(name)) {
        out.put(node, new Sim_port("out"+node));
        add_port(out.get(node));
      }
    }
  }

  public Station(String name,Globals g) {
    super(name);
    nodeName = name;
    globals = g;
    try {
      platforms = globals.platformMap.get(name);
    }
    catch (Exception e) {
      platforms = 2;
    }
      
    in = new Sim_port("in");
    add_port(in);
    out = new HashMap<String, Sim_port>();
    timeRecord = new HashMap<String, Double>();
    trainSpeed = new HashMap<String, Double>();
    trainRecord = new HashMap<String, List<String> >();
    trainsOccupying = new HashSet<String>();

    if (globals.adjL.containsKey(name)) {
      for (String node : globals.adjL.get(name)) {
        out.put(node, new Sim_port("out"+node));
        add_port(out.get(node));
      }
    }
  }

  // Station Node Application Logic

  public void body() {

    
    List<SignalPacket> requesters = new ArrayList<SignalPacket>();  // queue of access
                                                                    // requesters
    
    while (Sim_system.running()) {

      Sim_event e = new Sim_event();
      sim_get_next(e);
      int eventType = e.get_tag();
      LOG("Signal Type : "+eventType);

      switch(eventType) {
        case 0 : {          // Train Arrival

          trainMovementInfo info = (trainMovementInfo) e.get_data();
          
          List<String> route = info.route;
          String train_id = info.train_id;
          

          // Send release lock signal to the previous node.
          if (!info.generatingNode.equals("Source"))
            sim_schedule(out.get(info.generatingNode), globals.signalDelay, 3, train_id);

          // Send RTS to the next node in the route.
          if (route.size() > 0) {
            Double time = Sim_system.sim_clock();
            timeRecord.put(train_id, time);
            trainSpeed.put(train_id, info.speed); 
            trainRecord.put(train_id, route);
            trainsOccupying.add(train_id);
            String nextNode = route.get(0);
            SignalPacket sp = new SignalPacket(nodeName, train_id);
            Double haltTime = 0.0;
            try {
              haltTime = globals.haltMap.get(train_id).get(nodeName);
            }
            catch(Exception ex){
              haltTime = 2.0;
              System.err.println(nodeName +" -> " + train_id);
              System.err.println(ex);
              System.exit(0);
            }
            haltTime = haltTime*60;
            System.err.println(out.get(nextNode));
            System.err.println(nextNode+" - "+nodeName + " - " + train_id);
            sim_schedule(out.get(nextNode), globals.signalDelay+haltTime, 1, sp);
          }
          else {                                // Release the lock if the train terminates 
            trainsOccupying.remove(train_id);
            if (requesters.isEmpty()) {
              globals.LOCK.release(nodeName);
            } else {
              // schedule a requester
              SignalPacket requestingNode = requesters.remove(0);
              sim_schedule(out.get(requestingNode.nodeName), globals.signalDelay, 2, requestingNode);
            }
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
            List<String> route = trainRecord.get(msg.train_id);

            if (route.size() > 0) {             // Train has miles to go!
              String nextNode = route.remove(0);
              trainMovementInfo info = new trainMovementInfo(globals, route, nodeName, msg.train_id, globals.protocol, trainSpeed.get(msg.train_id));

              // Send the TRAIN to the next node
              Double delay = Sim_system.sim_clock() - timeRecord.get(msg.train_id);

              Double haltTime = 0.0;
              try {
                haltTime = globals.haltMap.get(msg.train_id).get(nodeName);
              }
              catch(Exception ex){
                haltTime = 2.0;
                System.err.println(nodeName +" -> " + msg.train_id);
                System.err.println(ex);
                System.exit(0);
              }
              haltTime = haltTime*60;


              OUT(msg.train_id, delay-haltTime);

              Double trainDelay = ((globals.linkDistance * 3600) / globals.speedHash.get(msg.train_id+nodeName)*1.0); 
                
              if (globals.protocol == 3) {
                Sim_normal_obj normalDist = new Sim_normal_obj("normal", globals.mean, globals.var);
                Double sample = normalDist.sample();
                sample = Math.min(10, sample);
                sample = Math.max(-10, sample);
                trainDelay = Math.max(0 , ((globals.linkDistance * 3600) / (globals.speedHash.get(msg.train_id+nodeName) + sample ) * 1.0)) ;
                // if(msg.delayed == true){
                //   trainDelay = ((globals.linkDistance * 3600) / (globals.speedHash.get(msg.train_id+nodeName)*(1 - globals.p3Beta/100.0)) * 1.0);  
                // }
              }

              if (globals.protocol == 4) {

                Sim_uniform_obj unif = new Sim_uniform_obj("unif", 0, (globals.frame_size));
                double p = unif.sample();
                double threshold = 1.0;

                if (p <= threshold) {
                  double max_delay = ((globals.beta * globals.congestion) / Math.log(globals.N * globals.dilation));
                  Sim_uniform_obj uniDist = new Sim_uniform_obj("uniform", 1, max_delay);
                  trainDelay += uniDist.sample();
                }

              }

              sim_schedule(out.get(nextNode), trainDelay, 0, info);

            }
            timeRecord.remove(msg.train_id);
            trainSpeed.remove(msg.train_id);
            trainRecord.remove(msg.train_id);

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
