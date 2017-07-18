#!/usr/bin/python
import json

with open('../train_delay_graph.json','r') as f :
	adjL = json.load(f)

	nodes = set()
	edgeL = set()

	for train in adjL :
		nodes.add(train)
		for reason in adjL[train] :
			nodes.add(reason)
			edgeL.add((train, reason))

	formattedJSON = {
		"nodes" : [],
		"links" : []
	}


	node_id = {}
	id = 0

	for node in nodes :
		node_id[node] = id
		id += 1
		formattedJSON["nodes"].append({
				"name" : node,
				"group" : node_id[node]
			})

	for edge in edgeL :
		formattedJSON["links"].append({
				"source" : node_id[edge[0]],
				"target" : node_id[edge[1]],
				"left" : False, 
				"right": True,
				"weight" : 5
			})

	with open('graphFile.json','w') as outfile :
		json.dump(formattedJSON, outfile)

