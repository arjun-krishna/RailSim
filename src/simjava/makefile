compile :
	javac -classpath ".:jars/simjava.jar:jars/json-simple-1.1.1.jar"	 Simulator.java

innerrun :
	java -classpath ".:jars/simjava.jar:jars/json-simple-1.1.1.jar" Simulator -Xms512m -Xmx4g ${ARGS} > statistics.dat

run : Simulator.class
	make ARGS="-f ../data/sr_data/" innerrun

clean :
	rm *.class -f
	rm */*.class -f