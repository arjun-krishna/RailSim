package models;
import java.util.*;

public class Lock {
  public HashMap<String, Integer> lock;
  public HashMap<String, Boolean> isStation;
  public HashMap<String, Integer> stationMax;

  public Lock() {
    lock = new HashMap<String, Integer>();
    stationMax = new HashMap<String, Integer>();
    isStation = new HashMap<String, Boolean>();
  }

  void init(HashMap<String, List<String> > adjL, HashMap<String, Integer > platformMap) {
    for (String node : adjL.keySet()) {
      if (node.charAt(0) == '$') {
        isStation.put(node, false);
      }
      else { 
        isStation.put(node, true);
        stationMax.put(node, platformMap.get(node));
      }
      lock.put(node, 0);
    }
  }

  Boolean available(String node) {
    if (isStation.get(node)) {
      Integer L = lock.get(node);
      if ( L < stationMax.get(node)) {
        return true;
      } else {
        return false;
      }
    } else {
      Integer L = lock.get(node);
      if (L == 0) {
        return true;
      } else {
        return false;
      }
    }
  }

  void acquire(String node) {
    Integer L = lock.get(node);
    L += 1;
    lock.put(node, L); 
  }

  void release(String node) {
    Integer L = lock.get(node);
    L -= 1;
    lock.put(node, L);
  }

}
