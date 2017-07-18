#!/usr/bin/python
import json
import sys
import math

nodes = set()
edgeL = {}
haltMap = {}
platformMap = {}
weeklySchedule = {}
speedMap = {}
typeMap = {}
args = sys.argv

if (len(args) == 1) :
    print 'usage : ./graphy.py {file}'
    exit(0)

with open(args[1]) as data_file:
    data = json.load(data_file)

for train in data :
    trainhalts  = {}
    speeddict = {}
    route = [s.replace(" ", "_") for s in train['route']]
    halts = [float(x) for x in train['halt']]
    distance = [int(x) for x in train['distance']] # TODO int
    speeds = [int(x) for x in train['speed']]
    weeklySchedule[train['train_id']] = train['weekly_schedule']
    typeMap[train['train_id']] = train['type']
    for i in range(0,len(route)):
        trainhalts[route[i]] = halts[i]
    haltMap[train['train_id']] = trainhalts
    speedMap[train['train_id']] = speeds

    for i in range(0, len(route)):
        station_code = route[i]
        if not station_code in platformMap:
            platformMap[station_code] = max(train['platform_count'][i],2)


    for i in xrange(len(route)-1) :
        A = (a,b) = (route[i], route[i+1])
        B = (b,a)
        nodes.add(a)
        nodes.add(b)
        diff = distance[i+1] - distance[i]

        if (A in edgeL) :
            if edgeL[A] < diff :
                edgeL[A] = diff
        else :
            edgeL[A] = diff

        if (B in edgeL) :
            if edgeL[B] < diff :
                edgeL[B] = diff

        else :
            edgeL[B] = diff



with open('station.json', 'w') as outfile:
    json.dump(list(nodes), outfile)
    outfile.close()

EL = []
for e in edgeL :
    EL.append(e)

with open('network.json', 'w') as outfile:
    json.dump(EL, outfile)
    outfile.close()


print 'Number of stations : ',len(nodes)

linkDistance = 3.0

sigId = 0

signalMap = {}

for edge in edgeL :
    if (edgeL[edge] <= linkDistance) :
        sigNodes = ['$'+str(sigId)]
        sigId += 1
        signalMap[edge] = sigNodes

    else :
        numNodes = math.ceil(edgeL[edge] / linkDistance) - 1
        sigNodes = []
        numNodes = int(numNodes)
        for i in range(numNodes) :
            sigNodes.append('$'+str(sigId))
            sigId += 1
        signalMap[edge] = sigNodes

print 'Number of Signalling Nodes : ',sigId


d = 0

for train in data :
    route = [s.replace(" ", "_") for s in train['route']]

    L = []

    for i in xrange(len(route)-1) :
        L += [route[i]] + signalMap[(route[i], route[i+1])]

    d = max(d,len(L)+1)

print 'Dilation (d) : ',d

N = 0
for train_id in weeklySchedule :
    N += sum(weeklySchedule[train_id])

print 'Number to trains, Scheduled (N) : ',N

adjL = {}

for edge in edgeL :
    (a,b) = edge

    sigNodes = signalMap[edge]
    sigNodes = [a] + sigNodes + [b]

    for i in xrange(len(sigNodes)-1) :
        try :
            adjL[sigNodes[i]].append(sigNodes[i+1])
        except KeyError:
            adjL[sigNodes[i]] = [sigNodes[i+1]]

        try :
            adjL[sigNodes[i+1]].append(sigNodes[i])
        except KeyError:
            adjL[sigNodes[i+1]] = [sigNodes[i]]

with open('graph.json', 'w') as outfile:
    json.dump(adjL, outfile)
    outfile.close()

with open('halt.json', 'w') as outfile:
    json.dump(haltMap, outfile)

with open('platform.json', 'w') as outfile:
    json.dump(platformMap, outfile)

with open('wod.json', 'w') as outfile:
    json.dump(weeklySchedule, outfile)

with open('type.json', 'w') as outfile:
    json.dump(typeMap, outfile)

def convert(train) :
    global signalMap
    obj = {}
    route = []
    original_route = [s.replace(" ", "_") for s in train['route']]

    for i in xrange(len(original_route)-1) :
        edge = (original_route[i], original_route[i+1])
        try :
            route += ([edge[0]] + signalMap[edge])
        except KeyError :
            route += ([edge[0]] + signalMap[(edge[1],edge[0])])

    route += [original_route[-1]]

    obj['route'] = route
    obj['speed'] = speedMap[train['train_id']]
    obj['train_id'] = train['train_id']
    obj['name'] = train['name']
    time_split = [int(x) for x in train['departure'][0].split("'")[1].split(':')]
    time = (time_split[0])*3600 + (time_split[1])*60 + time_split[2]
    obj['departure'] = time
    return obj


schedule = map(convert, data)

with open('schedule.json', 'w') as outfile:
    json.dump(schedule, outfile)


with open('signalMap.json', 'w') as outfile :
    data = dict((str(k), v) for k,v in signalMap.items())
    json.dump(data, outfile)

