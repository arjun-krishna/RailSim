compile :
	javac -classpath ".:jars/simjava.jar:jars/json-simple-1.1.1.jar"	 Simulator.java

run :
	java -classpath ".:jars/simjava.jar:jars/json-simple-1.1.1.jar" Simulator -Xms512m -Xmx4g ${ARGS} > statistics.dat

southrail : Simulator.class
	make ARGS="-f ../data/sr_data/" run

clean :
	rm *.class -f
