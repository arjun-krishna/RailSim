#!/usr/bin/python
import sys

file_name = '../statistics.dat'
if len(sys.argv) > 1: 
	file_name = sys.argv[1]



max_delay = {}
min_delay = {}

# To measure average Delay at a node
total_delay = {}
count = {}

# end-to-end delay of a train
end_to_end_delay = {}

# no_of_delay at station
no_of_delay = {}

# Congestion 
C = 0

delays = []		# To generate a histogram of delays

train_delay_graph = {}	# A directed graph, which indicates the reason for a train's delay
train_delay_place = {}	# For a (train, reason) it gives a list of places where it got delayed.

with open(file_name, 'r') as f :
	for line in f :
		line = line.strip()
		A = line.split('--')
		if A[0] == 'DELAY_STAT' :

			try :
				total_delay[A[2]] += float(A[3])
				count[A[2]] += 1

			except KeyError :
				total_delay[A[2]] = float(A[3])
				count[A[2]] = 1

			try :
				max_delay[A[2]] = max(max_delay[A[2]], float(A[3]))

			except KeyError :
				max_delay[A[2]] = float(A[3])

			try :
				min_delay[A[2]] = min(min_delay[A[2]], float(A[3]))

			except KeyError :
				min_delay[A[2]] = float(A[3])

			delays.append(float(A[3]))

			try :
				end_to_end_delay[A[1]] += float(A[3])

			except KeyError :
				end_to_end_delay[A[1]] = float(A[3])

		elif A[0] == 'TRAIN_DELAYED' :

			try :
				train_delay_graph[A[1]] =  list(set(train_delay_graph[A[1]] + A[3].split(',')[0:-1]))

			except KeyError :
				train_delay_graph[A[1]] = list(set(A[3].split(',')[0:-1]))
			
			try :
				no_of_delay[A[2]] += 1

			except KeyError :
				no_of_delay[A[2]] = 1

			reason_for_delay = A[3].split(',')[0:-1]
			for train in reason_for_delay :
				try :
					train_delay_place[(A[1], train)] += [A[2]]

				except KeyError :
					train_delay_place[(A[1], train)] = [A[2]]

			C = max(C, len(list(set(A[3].split(',')[0:-1]))))

print 'Congestion (C) : ',C

import json

with open('train_delay_graph.json','w') as f :
	json.dump(train_delay_graph, f)

import matplotlib.pyplot as plt

avg_delay = []
node_name = []

max_curve = []
min_curve = []

node_avg_delay = {}


# Calculate Average Delay 
delay_sum = 0
delay_count = 0
for node in total_delay :
	delay_sum += total_delay[node]
	delay_count += count[node]
	avg_delay.append(total_delay[node] / count[node]);
	node_name.append(node)

	max_curve.append(max_delay[node])
	min_curve.append(min_delay[node])

	node_avg_delay[node] = (total_delay[node] / count[node])

mean_delay = delay_sum/(delay_count*1.0)
variance = 0

max1node = node_avg_delay.iterkeys().next()
for node in node_avg_delay:
	max1node = node if node_avg_delay[max1node] < node_avg_delay[node] else max1node 
	variance += (node_avg_delay[node] - mean_delay)**2
variance = variance / (len(node_avg_delay)*1.0)

max2node = node_avg_delay.iterkeys().next()
for node in node_avg_delay :
	if node_avg_delay[node]<node_avg_delay[max1node] :
		max2node = node if node_avg_delay[max2node] < node_avg_delay[node] else max2node 

max3node = node_avg_delay.iterkeys().next()
for node in node_avg_delay :
	if node_avg_delay[node]<node_avg_delay[max2node]:
		max3node = node if node_avg_delay[max3node] < node_avg_delay[node] else max3node 

max4node = node_avg_delay.iterkeys().next()
for node in node_avg_delay :
	if node_avg_delay[node]<node_avg_delay[max3node]:
		max4node = node if node_avg_delay[max4node] < node_avg_delay[node] else max4node 
print 'node avg delays stations'
print max1node , node_avg_delay[max1node]
print max2node , node_avg_delay[max2node]
print max3node , node_avg_delay[max3node]
print max4node , node_avg_delay[max4node]
print 'mean delay: ' + str(delay_sum/(delay_count*1.0))
print 'Std Deviation: ' + str(variance**(0.5))
print node_name[avg_delay.index(max(avg_delay))] 
# Plot the average Delay
plt.plot(range(len(node_name)), avg_delay)
plt.tick_params(
    axis='x',          # changes apply to the x-axis
    which='both',      # both major and minor ticks are affected
    bottom='off',      # ticks along the bottom edge are off
    top='off',         # ticks along the top edge are off
    labelbottom='off') # labels along the bottom edge are off

plt.xlabel('Nodes')
plt.ylabel('Average Delay at a node (in s)')
plt.title('Average Delay vs Nodes')

# Plot the max, min Delay curve
# plt.plot(range(len(node_name)), max_curve)
# plt.plot(range(len(node_name)), min_curve)

plt.show()



# Historgram of observed delays
plt.ylim((0,1000))
plt.hist(delays,bins=100)
plt.title('Histogram of the delays observed')
plt.xlabel('Delay time (in s)')
plt.ylabel('Frequency of observation')

plt.show()

avg_end_to_end_map = {}

with open('../../data/sr_data/wod.json','r') as f :
	wod = json.load(f)

	end_to_end_delay_vector = []
	train_no_vector = []
	end_to_end_delay_sum = 0
	end_to_end_delay_count = 0
	for train_no in end_to_end_delay :
		L = wod[train_no]
		end_to_end_delay_sum+=end_to_end_delay[train_no]
		end_to_end_delay_count+=sum(L)
		avg_end_to_end_delay = end_to_end_delay[train_no] / sum(L)
		avg_end_to_end_map[train_no] = avg_end_to_end_delay
		end_to_end_delay_vector.append(avg_end_to_end_delay)
		train_no_vector.append(train_no)
	mean_delay = end_to_end_delay_sum/(end_to_end_delay_count*1.0)
	variance = 0
	max1node = avg_end_to_end_map.iterkeys().next()
	for node in avg_end_to_end_map:
		max1node = node if avg_end_to_end_map[max1node] < avg_end_to_end_map[node] else max1node 
		variance += (avg_end_to_end_map[node] - mean_delay)**2
	variance = variance / (len(avg_end_to_end_map)*1.0)

	max2node = avg_end_to_end_map.iterkeys().next()
	for node in avg_end_to_end_map :
		if avg_end_to_end_map[node]<avg_end_to_end_map[max1node]:
			max2node = node if avg_end_to_end_map[max2node] < avg_end_to_end_map[node] else max2node 

	max3node = avg_end_to_end_map.iterkeys().next()
	for node in avg_end_to_end_map :
		if avg_end_to_end_map[node]<avg_end_to_end_map[max2node]:
			max3node = node if avg_end_to_end_map[max3node] < avg_end_to_end_map[node] else max3node 

	max4node = avg_end_to_end_map.iterkeys().next()
	for node in avg_end_to_end_map :
		if avg_end_to_end_map[node]<avg_end_to_end_map[max3node]:
			max4node = node if avg_end_to_end_map[max4node] < avg_end_to_end_map[node] else max4node 
	print 'end_to_end delays train , delays'
	print max1node , avg_end_to_end_map[max1node]
	print max2node , avg_end_to_end_map[max2node]
	print max3node , avg_end_to_end_map[max3node]
	print max4node , avg_end_to_end_map[max4node]
	print 'mean end-to-end delay : ' + str(end_to_end_delay_sum/(end_to_end_delay_count*1.00))
	print 'Std Deviation: ' + str(variance**(0.5))
	plt.plot(range(len(end_to_end_delay_vector)), end_to_end_delay_vector)
	plt.tick_params(
    axis='x',          # changes apply to the x-axis
    which='both',      # both major and minor ticks are affected
    bottom='off',      # ticks along the bottom edge are off
    top='off',         # ticks along the top edge are off
    labelbottom='off') # labels along the bottom edge are off

	plt.xlabel('Trains')
	plt.ylabel('End to End delay (in s)')
	plt.title('End to End delays for trains')

	plt.show()

y = []
x = []

no_of_delays = 0
no_of_stations = 0
for station in no_of_delay :
	y.append(no_of_delay[station])
	x.append(station)
	no_of_delays+=no_of_delay[station]
	no_of_stations+=1
mean_delay = no_of_delays/( no_of_stations*1.0)
variance = 0

max1node = no_of_delay.iterkeys().next()
for node in no_of_delay:
	max1node = node if no_of_delay[max1node] < no_of_delay[node] else max1node 
	variance += (no_of_delay[node] - mean_delay)**2
variance = variance/(len(no_of_delay)*1.00)
max2node = no_of_delay.iterkeys().next()
for node in no_of_delay :
	if no_of_delay[node]<no_of_delay[max1node]:
		max2node = node if no_of_delay[max2node] < no_of_delay[node] else max2node 

max3node = no_of_delay.iterkeys().next()
for node in no_of_delay :
	if no_of_delay[node]<no_of_delay[max2node]:
		max3node = node if no_of_delay[max3node] < no_of_delay[node] else max3node 

max4node = no_of_delay.iterkeys().next()
for node in no_of_delay :
	if no_of_delay[node]<no_of_delay[max3node]:
		max4node = node if no_of_delay[max4node] < no_of_delay[node] else max4node 
print 'no of delays  , station v no_of_delays'
print max1node , no_of_delay[max1node]
print max2node , no_of_delay[max2node]
print max3node , no_of_delay[max3node]
print max4node , no_of_delay[max4node]
print 'mean no_of_delays : ' + str(no_of_delays/(no_of_stations*1.00))
print 'std. deviation : ' + str(variance**(0.5));
plt.plot(range(len(y)), y)
plt.tick_params(
  axis='x',          # changes apply to the x-axis
  which='both',      # both major and minor ticks are affected
  bottom='off',      # ticks along the bottom edge are off
  top='off',         # ticks along the top edge are off
  labelbottom='off') # labels along the bottom edge are off

plt.xlabel('Nodes')
plt.ylabel('Number of delays observed')
plt.title('Number of Delays caused per node')

plt.show()		


# Construct Delay Network
with open('../../data/sr_data/signalMap.json','r') as f :
	signalMap = json.load(f)
	signalMap = dict(( (k.split("'")[1], k.split("'")[3] ), v) for k,v in signalMap.items())

	delayNetwork = {
		"nodes" : [],
		"links" : []
	}

	for node in node_avg_delay :
		if node[0] is not '$' :
			delayNetwork["nodes"].append({
					"id" : node,
					"name" : node,
					"delay" : node_avg_delay[node]
				})

	for edge in signalMap :
		signalNodes = signalMap[edge]

		avglinkDelay = 0
		for node in signalNodes :
			try :
				avglinkDelay += node_avg_delay[node]
			except KeyError:
				continue

		delayNetwork["links"].append({
				"source" : edge[0],
				"target" : edge[1],
				"delay"	 : avglinkDelay
			})


	with open('delay_network.json', 'w') as f :
		json.dump(delayNetwork, f)

