package models;
import eduni.simjava.*;
import eduni.simjava.distributions.*;
import java.util.*;

// A class for the TRAIN packet in the network.

public class trainMovementInfo {
	public List<String> route;			 // future route information
	public String generatingNode;		 // where the train is coming from?
	public String train_id;					 // train identifier
	public Double speed;						 // train speed
	public int protocol;					 // protocol

	// Constructors
	// default : train_id -- 9&3/4 

	public trainMovementInfo(Globals globals, List<String> r, String gn, String id, int p, Double s) {
		route = r;
		generatingNode = gn;
		train_id = id;
		protocol = p;
		speed = s;
	}
}
// without creating new objects code TODO