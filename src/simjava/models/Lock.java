package models;
import java.util.*;

public class Lock {
  public Integer value;
  public Integer maxValue;

  public Lock(Integer max) {
    value = 0;
    maxValue = max;
  }

  Boolean available() {
    if(value < maxValue) {
      return true;
    }
    return false;
  }

  synchronized Boolean acquire() {
    Boolean nodeStatus = available();
    if(nodeStatus) {
      value += 1;
      return true;
    }
    return false;   
  }
  
  void release() {
    value -= 1;
  }  
}
