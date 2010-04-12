#! /usr/bin/env python

import string
import getopt
import sys
import pickle
import os
import re
import math

SEPARATOR=':'

def usage():
	print 'Usage: margin.py    '
	print '\t--boost-info=filename   file as output by jboost (-a -2 switch)'
	print '\t--data=filename         train/test filename'
	print '\t--spec=filename         spec filename'
	print '\t--booster=type          type={AdaBoost, LogLossBoost, NormalBoost, BrownBoost}'
	print '\t--framerate=num         an integer specifying framerate'


def erf(z):
	t = 1.0 / (1.0 + 0.5 * abs(z));
	ans = 1 - t * math.exp( -z*z   -   1.26551223 + \
				       t * ( 1.00002368 + \
				       t * ( 0.37409196 + \
				       t * ( 0.09678418 + \
				       t * (-0.18628806 + \
				       t * ( 0.27886807 + \
				       t * (-1.13520398 + \
				       t * ( 1.48851587 + \
				       t * (-0.82215223 + \
				       t * ( 0.17087277))))))))))
	if z < 0:
		ans = - ans
	return ans;


def get_weight_line(params, start, end, max_bin):
	weights = []
	s = c = c1 = c2 = theta = 0
	confidence = False
	if(params[0] == 'BrownBoost'):
		c = float(params[1])
		s = float(params[2]) #time remaining
	elif(params[0] == 'NormalBoost'):
		t = float(params[2]) # time left
		c = float(params[1])
		if t < 0:
			t = 0
		s = c-t #time played
		c1 = float(params[3])
		c2 = float(params[4])
		theta = float(params[5])
		confidence = params[6]
		
		
	num_steps = 100
	step = (end - start) / num_steps
	x = start
	for i in range(0,num_steps):
		if params[0] == 'BrownBoost' or params[0] == 'NormalBoost':
			mu = var = 0
			if s < 0:
				s = 0.0001
			if params[0] == 'BrownBoost': 
				mu = -s
				var = c
			elif params[0] == 'NormalBoost':
				mu = theta - c1 * (math.exp(-s) - math.exp(-c))
				var = c2 * (math.exp(-2*s) - math.exp(-2*c)) + 0.02

			norm = 1 / math.sqrt(var*math.pi)
			y = math.exp(-math.pow(x-mu,2)/(var)) * max_bin 
			p = (1 - erf((x-mu)/math.sqrt(var)))/2
			if (confidence):
				p = min(2*p, 1)
		elif params[0] == 'AdaBoost':
			y = math.exp(-x)
			p = y
		elif params[0] == 'LogLossBoost':
			y = 1.0/(1+math.exp(x))
			p = math.log(1.0 + math.exp(-x))
			
		weights.append((x,y,p))
		x += step
	return weights


def get_margin_hist(margins, is_cum):
	num_bins = 0
	if is_cum:
		num_bins = 200
	else:
		num_bins = 30

	marg_max = max(margins)
	marg_min = min(margins)
	bin_size = (marg_max - marg_min) / num_bins
	b = marg_min
	i = 0
	hist = []
	x_axis = []
	j = 0
	EPS = bin_size / 10000
	total_seen = 0
	while b <= marg_max:
		hist.append(0)
		while i < len(margins) and (margins[i] <= b + bin_size + EPS):
			i += 1
			hist[j] += 1
		x_axis.append(b + bin_size)
		if is_cum:
			tmp = hist[j]
			hist[j] += total_seen
			total_seen += tmp 
		b += bin_size
		j += 1

	s = 0
	if is_cum:
		s = max(hist)
	else:
		s = sum(hist)
	return [(x, float(h) / s)  for (x,h) in zip(x_axis,hist)]
		


def paste_gnuplot(num_iterations, framerate): 
	print 'Starting animated gif creation'
	os.system('convert -rotate  90 -delay 5 -loop 2 surfing*.eps surfing.gif')
	print 'Finished gif creation'
	print 'See surfing.gif for animation'



def write_gnuplot(lines, iter, datafile, params):

	print 'Writing gnuplot file'
	line_compare = lambda x,y : cmp(float(string.split(x,SEPARATOR)[1]), float(string.split(y,SEPARATOR)[1]))
	lines.sort(line_compare);
	margins = [ float(string.split(x,SEPARATOR)[1])    for x in lines]
	weights = [ float(string.split(x,SEPARATOR)[2])    for x in lines]
	potentials = [ float(string.split(x,SEPARATOR)[3])    for x in lines]



	marg_max = max(margins);
	marg_min = min(margins);
	lines = get_margin_hist(margins, True);
	max_bin = max([l[1] for l in lines])
	lines = [str(l[0]) + ' ' + str(l[1]) + '\n' for l in lines] 
	f= open('surfing_hist.dat', 'w')
	f.writelines(lines)
	f.close()

	weight_line = get_weight_line(params, marg_min, marg_max, max_bin)
#	lines = [ str(m) + ' ' + str(p) + ' ' + str(w) + '\n'  for (m,w,p) in weight_line]
	lines = [ str(m) + ' ' + str(p) + '\n'  for (m,w,p) in weight_line]
	f= open('surfing_weight.dat', 'w')
	f.writelines(lines)
	f.close()


	xrange = max([abs(marg_min), abs(marg_max)])
	yrange = max_bin

	epsoutlines = []
	epsoutlines.append('set terminal post\n')
	epsoutlines.append('set output \'surfing%05d.eps\'\n' % (iter))
	if params[0] == 'BrownBoost' or params[0] == 'NormalBoost':
		epsoutlines.append('set title "'+  params[0] +  ' ' + datafile + ' Surfing: Time Remaining ' + str(params[2]) + '" font "Times,20"\n')
	if params[0] == 'AdaBoost' or params[0] == 'LogLossBoost':
		epsoutlines.append('set title "'+  params[0] +  ' ' + datafile + ' Surfing: Iteration ' + str(iter) + '" font "Times,20"\n')
	epsoutlines.append('set key left top\n')
	epsoutlines.append('set yzeroaxis lt -1\n')
	if params[0] == 'NormalBoost':
		epsoutlines.append('set xrange [-1:1]\n')
		epsoutlines.append('set yrange [0:1]\n')
	else:
		epsoutlines.append('set xrange [%0.2f:%0.2f]\n' % (-xrange, xrange))
		epsoutlines.append('set yrange [0:1]\n')
	epsoutlines.append('set xlabel "Margin" font "Times,20"\n')
	epsoutlines.append('set ylabel "Cumulative Distribution" font "Times,20"\n')
#	epsoutlines.append('EPS = 0.00001\n')
#	epsoutlines.append('theta = %.5f\n' % (params[5]))
#	epsoutlines.append('set parametric \n')
#	epsoutlines.append('delta(t) = (x<=theta+EPS ? x>=theta-EPS ? 1.0 : 0 : 0 )\n')
	epsoutlines.append('plot "surfing_hist.dat" using 1:2 title "Margin CDF" with lines, ' + \
				'"surfing_weight.dat" using 1:2 title "Potential" with lines \n')
#				'delta(t) with lines \n')
#	if params[0]=='NormalBoost':
#		epsoutlines.append('set parametric\n')
#		epsoutlines.append('set trange [0:1]\n')
#		epsoutlines.append('replot ' + str(params[5]) + ', t \n')

	pngoutlines = epsoutlines[:]
	pngoutlines[0] = 'set terminal png notransparent small\n'
	pngoutlines[1] = ('set output \'surfing%05d.png\'\n' % (iter))

	gifoutlines = epsoutlines[:]
	gifoutlines[0] = 'set terminal gif notransparent\n'
	gifoutlines[1] = ('set output \'surfing%05d.gif\'\n' % (iter))


	f = open('surfing.png.gnuplot', 'w')
	f.writelines(pngoutlines)
	f.close()

	f = open('surfing.eps.gnuplot', 'w')
	f.writelines(epsoutlines)
	f.close()

	f = open('surfing.gif.gnuplot', 'w')
	f.writelines(gifoutlines)
	f.close()

	os.system('gnuplot surfing.eps.gnuplot')
	print 'Finished with iteration ' + str(iter)




def main():
    # Usage: see usage()
    # Looks at all the examples that have negative margins
    # the output can be used to find the examples that are probably mislabeled
    # and also the examples that might need more features

    try:
        opts, args= getopt.getopt(sys.argv[1:], '', ['boost-info=','data=','spec=', 'framerate=', 'booster='])
    except getopt.GetoptError, inst:
        print 'Received an illegal argument:', inst
        usage()
        sys.exit(2)

    boostfile = datafile = framerate = specfile = labelsfile = sample = iteration = booster = None
    for opt,arg in opts:
	    if (opt == '--booster'):
		    booster = arg
	    if (opt == '--framerate'):
		    framerate = int(arg)
	    if (opt == '--boost-info'):
		    boostfile = arg
	    if (opt == '--spec'):
		    specfile = arg
	    if (opt == '--data'):
		    datafile = arg


	

    if(boostfile == None or datafile == None or specfile == None):
	    print 'Need boosting.info, data, and spec file.'
	    usage()
	    sys.exit(2)
    if(framerate==None):
	    print 'Need frame rate.'
	    usage()
	    sys.exit(2)
    if(booster==None or (booster!='NormalBoost' and booster!='BrownBoost' and booster!='AdaBoost' and booster!='LogLossBoost')):
	    print 'Only accepts the same boosters (capitilization and all) from JBoost.'
	    usage()
	    sys.exit(2)
	    
    print 'Reading boosting.info file'
    f= open(boostfile,'r')
    data= f.readlines()
    f.close()
    num_elements= int((string.split(data[0],SEPARATOR)[1].split('=')[1]))
    num_iterations = int(string.split(string.split(data[-num_elements-1], SEPARATOR)[0], '=')[1])
    print 'Read ' + str(num_iterations) + ' iterations, each with ' + str(num_elements) + ' from ' + boostfile

    #os.system('rm surfing*.eps surfing*.png')

    for iter in range(0, num_iterations+1, framerate):
	    boost_params_str = string.split(data[iter*(num_elements+1)], SEPARATOR)[2].split('boosting_params=')[1]
	    if booster=='BrownBoost':
		    search_str = 'r='
		    total_time = boost_params_str[boost_params_str.index(search_str)+2 :].split(' ')[0]
		    search_str = 's='
		    time_left = boost_params_str[boost_params_str.index(search_str)+2 :].split(' ')[0]
		    params = ('BrownBoost', total_time, time_left)
	    elif booster=='NormalBoost':
		    search_str = 'c='
		    total_time = boost_params_str[boost_params_str.index(search_str)+2 :].split(' ')[0]
		    search_str = 's='
		    time_left = boost_params_str[boost_params_str.index(search_str)+2 :].split(' ')[0]
		    search_str = 'c1='
		    c1 = boost_params_str[boost_params_str.index(search_str)+len(search_str) :].split(' ')[0]
		    search_str = 'c2='
		    c2 = boost_params_str[boost_params_str.index(search_str)+len(search_str) :].split(' ')[0]
		    search_str = 'theta='
		    theta = boost_params_str[boost_params_str.index(search_str)+len(search_str) :].split(' ')[0]
		    search_str = 'posc='
		    posc = boost_params_str[boost_params_str.index(search_str)+len(search_str) :].split(' ')[0]
		    search_str = 'negc='
		    negc = boost_params_str[boost_params_str.index(search_str)+len(search_str) :].split(' ')[0]
		    search_str = 'posc1='
		    posc1 = boost_params_str[boost_params_str.index(search_str)+len(search_str) :].split(' ')[0]
		    search_str = 'negc1='
		    negc1 = boost_params_str[boost_params_str.index(search_str)+len(search_str) :].split(' ')[0]
		    search_str = 'posc2='
		    posc2 = boost_params_str[boost_params_str.index(search_str)+len(search_str) :].split(' ')[0]
		    search_str = 'negc2='
		    negc2 = boost_params_str[boost_params_str.index(search_str)+len(search_str) :].split(' ')[0]
		    search_str = 'postheta='
		    postheta = boost_params_str[boost_params_str.index(search_str)+len(search_str) :].split(' ')[0]
		    search_str = 'negtheta='
		    negtheta = boost_params_str[boost_params_str.index(search_str)+len(search_str) :].split(' ')[0]
		    search_str = 'asymmetric='
		    asymmetric = boost_params_str[boost_params_str.index(search_str)+len(search_str) :].split(' ')[0]
		    use_asym = False
		    if asymmetric == 'true' or asymmetric == 'True' or asymmetric == '1':
			    use_asym = True
		    search_str = 'confidence='
		    confidence = boost_params_str[boost_params_str.index(search_str)+len(search_str) :].split(' ')[0]
		    use_conf = False
		    if confidence == 'true' or confidence == 'True' or confidence == '1':
			    use_conf = True
		    params = ('NormalBoost', total_time, time_left, c1, c2, theta, use_conf)
	    else:
		    params = (booster,)
	    lines = [x   for x in data[iter*(num_elements+1)+1:(iter+1)*(num_elements+1)]]
	    write_gnuplot(lines, iter, datafile, params)

    paste_gnuplot(num_iterations, framerate)
	    
	    

if __name__ == "__main__":
    main()

