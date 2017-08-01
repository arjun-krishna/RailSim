package models;
import eduni.simjava.*;
import eduni.simjava.distributions.*;
import java.util.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import java.io.*;

public class Globals {

  /* Network Parameters */
  public int linkDistance = 3;          // distance between signal nodes(in km)
  public double signalDelay = 0.0;   // Signal Delay for linkDistance
  public double trainDelay = 240;         // Train Delay for linkDistance [Fixed speed simulation]

  public Double SPEED_Exp = 45.0;
  public Double SPEED_Pass = 45.0;
  public Double SPEED_SF = 45.0;

  /* LMR Protocol */
  public int protocol = 3;
  // 0 - Default [No LMR]
  // 1 - delay at source [LMR]
  // 2 - every train sent with a initial velocity picked by a normal distribution
  // 3 - every link train velocity changed by a normal distribution
  // 4 - delay at all nodes with a normal distribution [RDIN]


  public Integer congestion = 4;
  public Integer N = 2712;
  public double dilation = 356;

  /*protocol 1*/
  public double alpha = 1000.0;

  /*protocol 2(or)3*/
  public double mean = 0;
  public double var = 5;
  public double p3Beta = 0.15;

  /*protocol 4*/
  public double beta = 300;
  public double frame_size = Math.log(dilation);



  // Adjacency List, station to reachable stations
  // This defines the Railway Station Network

  public HashMap<String, List<String> > adjL;
  public HashMap<String, HashMap<String,Double> > haltMap;
  public HashMap<String, Integer > platformMap;
  public HashMap<String, Double > speedMap;
  public HashMap<String, List<Integer> > weeklySchedule;
  // Schedule object contains
  // the route of the train, hashed to the a timestamp at which the
  // train is scheduled.
  public HashMap<String , Long> speedHash;
  public HashMap<Double, List<String> > Schedule;
  public HashMap<Double, String> TrainIdentifier;

  // Lock of all nodes
  public Lock LOCK;

  // Constructor
  // param : args [contains file to load train route data]

  public Globals(String[] args) {

    adjL = new HashMap<String, List<String> >();
    Schedule = new HashMap<Double, List<String> >();
    TrainIdentifier = new HashMap<Double, String>();
    haltMap = new HashMap<String, HashMap<String,Double> >() ;
    platformMap = new HashMap<String, Integer >();
    speedMap = new HashMap<String, Double >();
    speedHash = new HashMap<String, Long >();
    weeklySchedule = new HashMap<String, List<Integer> >();
    LOCK = new Lock();
    this.loadDataFromJson(args);

  };

  // loadDataFromJson
  // description : Reads a JSON files 'graph.json' and 'schedule.json'
  //               in the folder specified,
  //  'graph.json'   -> Has the adjacency List
  // 'schedule.json' -> Has the train's route and time schedule

  public void loadDataFromJson(String args[]) {

    // signalCount -- counts the number of signalling nodes added
    int signalCount = 0;

    try {
      JSONParser parser = new JSONParser();
      List list = Arrays.asList(args);
      int value;
      String folder;
      if ((value=list.indexOf("-f")) != -1) {
       folder = args[value+1];
      }
      else {    // default file to load data
        folder = "../data/simple_test/";
      }

      JSONObject network = (JSONObject) parser.parse(new FileReader(folder + "graph.json"));
      for (Object key : network.keySet()) {
        String node = (String) key;
        Object adjacentNodes = network.get(node);
        List<String> neighbours = (List<String>) (adjacentNodes);
        adjL.put(node, neighbours);
      }

      Sim_normal_obj normalDist = new Sim_normal_obj("normal", 0, 1e-4);

      JSONArray data = (JSONArray) parser.parse(new FileReader(folder + "schedule.json"));
      

      for (Object elem : data) {

        JSONObject train = (JSONObject) elem;
        List<String> route  = (List<String>) train.get("route");
        List<Long> speeds = (List<Long>) train.get("speed");

        Double timestamp;
        if(train.get("departure") instanceof Long) 
          timestamp = ((Long) train.get("departure")).doubleValue();
        else 
          timestamp = (Double) train.get("departure");
        timestamp += normalDist.sample();
        
        Schedule.put(timestamp, route);

        String train_id = (String) train.get("train_id");

        int currStationIndex = 0;
        for(String node : route){
          speedHash.put(train_id + node, speeds.get(currStationIndex));
          if(!node.contains("$")){
            currStationIndex+=1;
          }
        }
        TrainIdentifier.put(timestamp, train_id);
      }

      JSONObject haltInfo = (JSONObject) parser.parse(new FileReader(folder + "halt.json"));
      for (Object key : haltInfo.keySet()) {
        String train_id = (String) key;
        HashMap<String,Double> trainHaltsInfo = (HashMap<String,Double>) haltInfo.get(train_id);
        haltMap.put(train_id, trainHaltsInfo);
      }

      JSONObject platformsInfo = (JSONObject) parser.parse(new FileReader(folder + "platform.json"));
      for (Object key : platformsInfo.keySet()) {
        String stationCode = (String) key;
        Integer platformCount = Integer.parseInt(""+platformsInfo.get(stationCode));
        platformMap.put(stationCode, platformCount);
      }

      JSONObject typeInfo = (JSONObject) parser.parse(new FileReader(folder + "type.json"));
      for (Object key : typeInfo.keySet()) {
        String trainId = (String) key;
        String type = (String) typeInfo.get(trainId);
        if(type.equals("SF")){
          speedMap.put(trainId, SPEED_SF);
        }
        else if(type.equals("Pass")){
          speedMap.put(trainId, SPEED_Pass);
        }
        else if(type.equals("Exp")){
          speedMap.put(trainId, SPEED_Exp);
        }
        else {
          speedMap.put(trainId, SPEED_Pass);
        }
      }

      JSONObject weeklySched = (JSONObject) parser.parse(new FileReader(folder + "wod.json"));
      for (Object key : weeklySched.keySet()) {
        String train_id = (String) key;
        weeklySchedule.put(train_id,(List<Integer>) weeklySched.get(train_id));
      }

      LOCK.init(adjL, platformMap);
    }
    catch(Exception e) { // Some error occured while reading!
      System.err.println(e);
    }
  }
};
