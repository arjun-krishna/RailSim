import requests
from bs4 import BeautifulSoup as BS
import sys
import json
def process_route(name, routelist):
	dictdata = {'route':[],'distance':[],'halt':[],'platform_count':[], 'speed':[]}
	for station_data in routelist:
		# print routelist
		dictdata['route'].append(station_data['st_code'])
		dictdata['distance'].append(float(station_data['dist']))
		dictdata['halt'].append(station_data['halt'])
		dictdata['platform_count'].append(station_data['platform_count'])
		dictdata['speed'].append(station_data['speed'])
	if len(dictdata['route']) > 0:
		dictdata['src'] = dictdata['route'][0]
		dictdata['dst'] = dictdata['route'][-1]
		dictdata['departure'] = routelist[0]['departure']
	dictdata['name'] = name
	return dictdata

def get_route(num):
	r = requests.get('http://indiarailinfo.com/shtml/list.shtml?LappGetTrainList/'+str(num)+'/0/0/0?')
	fbs = BS(r.text, 'html.parser')
	table = fbs.findAll('table')
	if len(table) > 0 and int(table[0].attrs['numrows']) == 1: 
		tr = table[0].tr
		num = tr.findAll('td')[0].string
		r = requests.get('http://indiarailinfo.com/train/timetable/all/' + str(num))
		bs = BS(r.text, 'html.parser')
		name = bs.find('div', {'id':'Div'}).find('h1').find(text=True, recursive=False).split('/')[1]
		table = bs.findAll('table', {'class':'newschtable'})
		station_list = []
		failed_list = []
		if len(table) > 0 :
			table = table[0]
			trows = table.findAll('tr')
			for row in trows:
				if not row.has_attr('style'):
					continue
				tds = row.findAll('td')
				station_data = {}
				station_data['platform_count'] = 2
				station_data['speed'] = '50'
				if len(tds) > 3 and tds[3].a:
					data = tds[3].a['title']
					step1 = data.split('|')[0]
					station_data['st_name'] = step1.split('/')[1].split('(')[0]
					try :
						station_data['platform_count'] = int(data.split('(')[1].split(' ')[0])		
						if "(" in step1 :
							station_data['platform_count'] = int(step1.split('(')[1].split(' ')[0])
						else :
							failed_list.append(station_data['st_name'])
					except :
						pass
					station_data['speed'] = tds[14].text
					if station_data['speed'] == '-':
						station_data['speed'] = '50'
					station_data['dist'] 	= tds[13].text
					station_data['zone'] 	= tds[16].text
					try:
						station_data['halt'] 	= float(tds[10].text.split('m')[0])
					except :
						station_data['halt'] 	= 0
					# print tds[8].text
					station_data['departure'] 	= 'NULL'
					try:
						station_data['departure'] = tds[8].find('b').text
					except :
						import re
						matching = re.match(r'([0-9])+:([0-9])+\b', tds[8].text)
						if matching:
							station_data['departure'] =  tds[8].text
						else :
							print tds[8].text
				if len(tds) > 2 and tds[2].a:
					station_data['st_code'] = tds[2].a.string
				# if row.has_attr('class'):
					# print row.attrs['class']
				if row.has_attr('class') and 'substn' in row.attrs['class'] :
					station_data['is_substation'] = True
				else :
					station_data['is_substation'] = False
				station_list.append(station_data)

		# for station_data in station_list:
		# 	if station_data['st_name'] not in failed_list:
		# 		# print station_data['is_substation'], station_data['st_code'], station_data['st_name'], station_data['platform_count']
		# 	else :
		# 		# print 'This has no platform count : ', station_data['st_name']
		# 	# pass
		final_list = []
		orig_list = []
		found_SR = 0
		for station_data in station_list:
			if (found_SR == 0) and (not station_data['is_substation']) and (station_data['zone'] == 'SR'):
				found_SR = 1
			if (found_SR == 1) and (not station_data['is_substation']) and (station_data['zone'] != 'SR'):
				found_SR = 2
			if found_SR == 1 and (not station_data['is_substation']) and (station_data['zone'] == 'SR'):
				final_list.append(station_data)
			if (not station_data['is_substation']) and (station_data['zone'] == 'SR'):
				orig_list.append(station_data)
		if (final_list != orig_list) :
			print name
		else :
			print 'ok!!!'
		return process_route(name,final_list)
	else :
		return {'error' : 'Getroute'}



def get_dep_days(elem):
	tds = elem.findAll('td')
	data = [0 for x in range(0,7)]
	for i in range(0,7):
		# print tds[i].text
		if tds[i].text != u'\xa0':
			data[i] = 1
	return data

final_raillist = []
text = ''
with open('data.html', 'r') as f:
	text = f.read()
	f.close()
bs = BS(text, 'html.parser')
train_map = {}
# try :
table = bs.find('table',{'class':'srhres'})
trs = table.findAll('tr')
with open('testing.json','w') as f:
	f.write('[')
	for i in range(0, len(trs)-1) :
		tr = trs[i]
		if not tr.has_attr('style') or not tr.has_attr('id'): #table header content, additionl
			pass
		else :
			print str(i) +'/' + str(len(trs))
			tds = tr.findAll('td')
			train_map[tds[0].text] = {}
			tdict = train_map[tds[0].text]
			tdict['type'] 	= tds[2].text
			tdict['name'] 	= tds[1].text
			tdict['region'] = tds[3].text
			tdict['src'] 	= tds[7].text
			tdict['dest'] 	= tds[9].text
			tdict['halts']	= tds[12].text
			tdict['dep_days'] = get_dep_days(tds[13])
			# sys.exit(0)
			tdict['dist']	= tds[15].text
			# print tdict
			# nexttr = trs[i+1]
			# print nexttr
			# nexttable= nexttr.find('table')
			# tabletrs = nexttable.findAll('tr')
			# timetabletr = tabletrs[7]
			# link = timetabletr.find('a')
			# print tdict['type']
			if tdict['type'] == 'SF' or tdict['type'] == 'Exp' or tdict['type'] =='Pass':
				finaldict = get_route(tds[0].text)
				# print tds[0].text
				# print finaldict
				finaldict['train_id'] = tds[0].text
				finaldict['weekly_schedule'] = tdict['dep_days']
				finaldict['type'] = tdict['type']
				f.write(json.dumps(finaldict))
				f.write(',\n')
				# final_raillist.append(finaldict)
			# sys.exit(0)
	f.write(']')
	f.close()
# except :
# 	print 'error'
import json
jsobj = json.dumps(final_raillist)
print jsobj