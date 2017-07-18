
Stations = ["Winterfell", "DreadFort", "CastleBlack", "King's Landing", "Dorne", "Old town", "Mereen", "Pyke", "Vale", "The North", "Astapor", "Dragonstone", "Vaes Dothrak", "Pentos"]

import random
import json

d = {}

for placeX in Stations :
	for placeY in Stations :
		d[(placeX,placeY)] = int(10 + (random.random()*100))
		d[(placeY,placeX)] = d[(placeX,placeY)]

routes = []

for i in xrange(100) :
	route = []
	for place in Stations :
		p = random.random()
		if (p > 0.7) :
			route.append(place)
	random.shuffle(route)
	if len(route) > 0 :
		routes.append(route)
	else :
		continue

info = []

for route in routes :
	routeInfo = {}
	routeInfo['src'] = route[0]
	routeInfo['dst'] = route[-1]
	routeInfo['distance'] = []
	routeInfo['route'] = []
	distance = 0
	for i in range(len(route) - 1) :
		routeInfo['distance'].append(str(distance))
		distance += d[(route[i], route[i+1])]
		routeInfo['route'].append(route[i])
	routeInfo['route'].append(route[-1])
	routeInfo['distance'].append(str(distance))
	routeInfo['departure'] = ["11:00:00"]
	info.append(routeInfo)
# print len(info)
jsobj = json.dumps(info)
print jsobj