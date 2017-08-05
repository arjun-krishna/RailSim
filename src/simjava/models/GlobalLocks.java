package models;
import java.util.*;

public class GlobalLocks {
  public HashMap<String, Lock> lockMap;

  public GlobalLocks() {
    lockMap = new HashMap<String, Lock>();
  }

  void init(HashMap<String, List<String> > adjL, HashMap<String, Integer > platformMap) {
    for (String node : adjL.keySet()) {
      if (node.charAt(0) == '$') {
        Lock lock = new Lock(1);
        lockMap.put(node, lock);
      }
      else { 
        Lock lock = new Lock(platformMap.get(node)); // number of platforms
        lockMap.put(node, lock);
      }
    }
  }

  Boolean available(String node) {
    return lockMap.get(node).available();
  }

  Boolean acquire(String node) {
    return lockMap.get(node).acquire();
  }

  void release(String node) {
    lockMap.get(node).release();
  }

  List<Boolean> availabilityStatus(List<String> stationNames){
    List<Boolean> avlStatus = new ArrayList<Boolean>();
    for(String node:stationNames){
      avlStatus.add(available(node));
    }
    return avlStatus;
  }
}
