#! /usr/bin/env python

import string
import getopt
import sys
import pickle
import os
import re


SEPARATOR = ':'

def usage():
	print 'Usage: margin.py    '
	print '\t--boost-info=filename  margins file as output by jboost (-a -2 switch)'
	print '\t--data=filename        train/test filename'
	print '\t--spec=filename        spec filename'
	print '\t--iteration=i,j,k,...  the iterations to inspect, no spaces between commas'
	print '\t--sample               are we interested in a sample of false pos/neg'

def sortByColumns(list2d, column_indices, in_place=False):
    aux = [ [ sl[i] for i in column_indices ] + [sl] for sl in list2d ]
    aux.sort()
    aux = [ sl[-1] for sl in aux ]
    if in_place: list2d[:] = aux
    else: return aux

def writeList(list, filename):
	f= open(filename, 'w')
	for lines in range(len(list)):
		f.write(str(list[lines][0]))
		f.write(list[lines][1])

	f.close()


def write_files(examples, datafile, sample, margins, falsepos, truepos, falseneg, trueneg):

    f= open(datafile + '.margin.report', 'w')
    f.write('total\t tp\t tn\t fp\t fn\n')
    f.write(str(len(margins)) + \
	    '\t ' + str(len(truepos)) + \
	    '\t ' + str(len(trueneg)) + \
	    '\t ' + str(len(falsepos)) + \
	    '\t ' + str(len(falseneg)) + \
	    '\n'
	    )
    f.write('Sensitivity: ' +  str(1-float(len(falseneg))/(len(truepos)+len(falseneg))) + '\n')
    f.write('False Pos Rate: ' + str(float(len(falsepos))/(len(trueneg)+len(falsepos))) + '\n')

    f.write('True pos: ' + str(len(truepos)) + '\n')
    f.write('False pos: ' + str(len(falsepos)) + '\n')
    f.write('True neg: ' + str(len(trueneg)) + '\n')
    f.write('False neg: ' + str(len(falseneg)) + '\n')

    
    list= [[margins[w],examples[w]] for w in falseneg.keys()]
    sortByColumns(list, [0], True)
    writeList(list, datafile + '.false.neg')

    if (sample!=None):
	    sample= [list[x] for x in range(0, len(list), 50)]
	    f.write('Sampling '+ str(len(sample)) + ' false negative examples.\n')
	    writeList(sample, datafile + '.false.neg.samples')



    list= [[margins[w],examples[w]] for w in falsepos.keys()]
    sortByColumns(list, [0], True)
    writeList(list, datafile+'.false.pos')

    if (sample!=None):
	    sample= [list[x] for x in range(0, len(list), 5)]
	    f.write('Sampling '+ str(len(sample)) + ' false positive examples.\n')
	    writeList(sample, datafile + '.false.pos.samples')


    f.close()

    os.system('cat ' + datafile +'.margin.report')




def write_gnuplot(m_list, iters, datafile):

	margin_list = []
	for i in range(len(iters)):
		scores = m_list[i]
		m = m_list[i]
		margins = m[:]
		margins.sort()
		margin_list.append(margins)

	lines = []
	margin_list_maxes = []
	for i in range(len(margin_list)):
		margins = margin_list[i]
		margin_list_maxes.append(max(margins))


	for w in range(len(margin_list[0])):
		line = ""
		line += str(float(w)/len(margin_list[0]))
		for i in range(len(margin_list)):
			margins = margin_list[i]
			marg_max = margin_list_maxes[i]
			line += ' '
			line += str(margins[w]/marg_max)
		line += ' \n' 
		lines.append(line)

	f= open('margin.dat', 'w')
	f.writelines(lines)
	f.close()


	epsoutlines = []
	epsoutlines.append('set terminal post\n')
	epsoutlines.append('set output \'margin.eps\'\n')
	epsoutlines.append('set title "' + datafile + ' Margins" font "Times,20"\n')
	epsoutlines.append('set key left top\n')
	epsoutlines.append('set size 0.8, 0.8\n')
	epsoutlines.append('set xlabel "Margin" font "Times,20"\n')
	epsoutlines.append('set ylabel "Cumulative Distribution" font "Times,20"\n')
	out = ''
	out += 'plot '
	for i in range(len(iters)):
		out += ' "margin.dat" using ' + str(i+2) + ':1'
		out += ' title \'Iteration: ' + str(iters[i]) + '\' with lines ,'
	out = out[:-1]
	out += '\n'
	epsoutlines.append(out)

	pngoutlines = epsoutlines[:]
	pngoutlines[0] = 'set terminal png notransparent small\n'
	pngoutlines[1] = 'set output \'margin.png\'\n'

	
	pngfilename = 'margin.png.gnuplot'
	epsfilename = 'margin.eps.gnuplot'

	f = open(pngfilename, 'w')
	f.writelines(pngoutlines)
	f.close()

	f = open(epsfilename, 'w')
	f.writelines(epsoutlines)
	f.close()

	os.system('gnuplot ' + pngfilename)
	os.system('gnuplot ' + epsfilename)
	os.system('epstopdf margin.eps')



def getLabels(specfilename):
	f = open(specfilename, 'r')
	lines = f.readlines()
	f.close()

	label1 = ''
	label2 = ''

	p = re.compile('[ \t]*labels[ \t]*(\\(.*\\), \\(.*\\))')

	for line in lines:
		if line.find('labels', 0, 5) > 0:
			print line
			m = p.match(line)
			

	label1 = m.group
	return label1, label2
	

def process_data(examples, labels, margins, datafile, sample):

	print 'Examples ' + str(len(examples))
	print 'Margins  ' + str(len(margins))
	print 'Generating margins'

	print 'Matching margins with data'

	falsepos = dict([(w,[margins[w],examples[w],labels[w]])
		    for w in range(len(margins))
		    if margins[w] < 0.0 and labels[w].strip() == '-1'])
	truepos  = dict([(w,[margins[w],examples[w],labels[w]])
		for w in range(len(margins))
		if margins[w] > 0.0 and labels[w].strip() == '+1'])


	falseneg = dict([(w,[margins[w],examples[w],labels[w]])
		for w in range(len(margins))
		if margins[w] < 0.0 and labels[w].strip() == '+1'])
	trueneg  = dict([(w,[margins[w],examples[w],labels[w]])
		for w in range(len(margins))
		if margins[w] > 0.0 and labels[w].strip() == '-1'])


#        for i in range(len(margins)):
#		print '%d\t %s\t %.2f\t %.2f' % (i, labels[i].strip(), margins[i])

	write_files(examples, datafile, sample, margins, falsepos, truepos, falseneg, trueneg)
	




def main():
    # Usage: see usage()
    # Looks at all the examples that have negative margins
    # the output can be used to find the examples that are probably mislabeled
    # and also the examples that might need more features

    try:
        opts, args= getopt.getopt(sys.argv[1:], '', ['boost-info=', 'data=', 'spec=', 'iteration=', 'sample'])
    except getopt.GetoptError, inst:
        print 'Received an illegal argument:', inst
        usage()
        sys.exit(2)

    boostfile = datafile = specfile = labelsfile = sample = iteration = None
    for opt,arg in opts:
	    if (opt == '--boost-info'):
		    boostfile = arg
	    if (opt == '--spec'):
		    specfile = arg
	    elif (opt == '--data'):
		    datafile = arg
	    elif (opt == '--labels'):
		    labelsfile = arg
	    elif (opt == '--iteration'):
		    iteration = arg
	    elif (opt == '--sample'):
		    sample = True
	

    if(boostfile == None or datafile == None or specfile == None):
	    print 'Need score, data, and spec file.'
	    usage()
	    sys.exit(2)
    
    print 'Reading data'
    f= open(datafile,'r')
    examples= f.readlines()
    f.close()
    
    print 'Reading boosting info'
    f= open(boostfile,'r')
    data= f.readlines()
    f.close()
    margin_elements= int((string.split(data[0],SEPARATOR))[1].split('=')[1])
    
    def get_margin(line):
	    m = line.split(SEPARATOR)[1]
	    m.replace(']','')
	    m.replace(';','')
	    m.replace(SEPARATOR,'')
	    m.replace(' ','')
	    m.replace('\t','')
	    return float(m)

    def get_label(line):
	    m = line.split(SEPARATOR)[-2]
	    m.replace(']','')
	    m.replace(';','')
	    m.replace(SEPARATOR,'')
	    m.replace(' ','')
	    m.replace('\t','')
	    return m

    # if iterations are not specified on command line, we do the last iteration
    iters = [1]
    iters[0] = len(data) / (margin_elements+1)

    margin_list = []
    if (iteration != None):
	    iters = map(int, [x.strip() for x in iteration.split(',')])
	    for iter in iters:
		    lines = [x for x in data[iter*(margin_elements+1)+1:(iter+1)*(margin_elements+1)]]
		    margins = map(get_margin, lines)
		    margin_list.append(margins)
    else:
	    lines = [x for x in data[-margin_elements:]]
	    margins = map(get_margin, lines)
	    margin_list.append(margins)

    labels = map(get_label, [x for x in data[-margin_elements:]])
    process_data(examples, labels, margin_list[len(margin_list)-1], datafile, sample)

    write_gnuplot(margin_list, iters, datafile)
    

if __name__ == "__main__":
    main()
