import json
flist = []
errlist = []
with open('testing.json') as f:
	jobj = json.load(f)
	for dict in jobj:
		if not 'error' in dict and 'departure' in dict and dict['departure'] != 'NULL' and len(dict['route']) > 1:
			dict['departure'] = ["'" + dict['departure'] + ':00' + "'"]
			flist.append(dict)
		else :
			errlist.append(dict)
with open('final.json', 'w') as outfile:
    json.dump(flist, outfile)
with open('err.json', 'w') as outfile:
    json.dump(errlist, outfile)