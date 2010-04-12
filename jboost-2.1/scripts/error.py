#! /usr/bin/env python

import string
import getopt
import sys
import os

def usage():
	print 'Usage: error.py    '
	print '\t--info=info_file    scores file as output by jboost'
	print '\t--logaxis           should the axis be log-scaled (default: false)'
	print '\t--bound             show the bound on training error'
	print '\t--separate          separate the positive and negative examples'
	print '\t--boost-info=file  The boosting.info file for a data set'

SEPARATOR = ':'

def get_margin(line):
	m = line.split(SEPARATOR)[1]
	m.replace(']','')
	m.replace(';','')
	m.replace(SEPARATOR,'')
	m.replace(' ','')
	m.replace('\t','')
	return float(m)

def get_score(line):
	m = line.split(SEPARATOR)[2]
	m.replace(']','')
	m.replace(';','')
	m.replace(SEPARATOR,'')
	m.replace(' ','')
	m.replace('\t','')
	return float(m)

def getErrorsSingleIter(data):
	tp = 0
	fp = 0
	tn = 0
	fn = 0

	scores = map(get_score, data)
	margins = map(get_margin, data)
	for score, margin in zip(scores, margins):		
		if score < 0 and margin < 0:
			fn += 1
		if score < 0 and margin > 0:
			tn += 1
		if score > 0 and margin < 0:
			fp += 1
		if score > 0 and margin > 0:
			tp += 1
	return (tp,fp,tn,fn)					

def getErrors(boostfilename):
    f= open(boostfilename,'r')
    data= f.readlines()
    f.close()
    score_elements = int((string.split(data[0],SEPARATOR))[1].split('=')[1])
    num_iterations = int((string.split(data[-score_elements-1],SEPARATOR))[0].split('=')[1])
    print num_iterations

    errors = []
    for iter in range(num_iterations+1):
	    tp,fp,tn,fn = getErrorsSingleIter([x for x in data[iter*(score_elements+1)+1:(iter+1)*(score_elements+1)]])
	    print iter,tp,fp,tn,fn
	    ret = {}
	    if tp + fn > 0:
		    ret['recall'] = ret['sensitivity'] = float(tp) / (tp + fn)
	    else:
		    ret['recall'] = ret['sensitivity'] = 0
	    if tp + fp > 0:
		    ret['precision'] = float(tp) / (tp + fp)
	    else:
		    ret['precision'] = 0
	    if fp + tn > 0:		    
		    ret['fpr'] = float(fp) / (fp + tn)
	    else:
		    ret['fpr'] = 0
	    ret['specificity'] = 1 - ret['fpr']
	    ret['neg_err'] = ret['fpr']
	    ret['pos_err'] = 1 - ret['sensitivity']
	    ret['err'] = float(fp+fn) / (fp+tp+fn+tn)
	    ret['iter'] = iter
	    errors.append(ret)
    return errors


def main():
    # Usage: see usage()

    try:
	    opts, args= getopt.getopt(sys.argv[1:], '', ['info=','logaxis','bound','separate','boost-info='])
    except getopt.GetoptError:
	    usage()
	    sys.exit(2)
	    
    info_filename = logaxis = showbound = show_separate = boost_filename = None
    for opt,arg in opts:
	    if (opt == '--info'):
		    info_filename = arg
	    if (opt == '--boost-info'):
		    boost_filename = arg
	    if (opt == '--info'):
		    score_filename = arg
	    if (opt == '--separate'):
		    show_separate = True
	    if (opt == '--logaxis'):
		    logaxis = True
	    elif (opt == '--bound'):
		    showbound = True
	

    if show_separate and boost_filename==None:
	    print 'ERROR: Need to specify score and margin file if showing positive and negative elements separately'
	    usage()
	    sys.exit(2)	    
	    
    if info_filename == None:
	    print 'ERROR: Need to specify info file'
	    usage()
	    sys.exit(2)

    print 'Parsing info file'
    f = open(info_filename,'r')
    lines = f.readlines()
    f.close()


    def get_info_data(line):
	    list = line.split('\t')
	    ret = {}
	    ret["iter"] = list[0]
	    ret["bound"] = list[1]
	    ret["train"] = list[2]
	    ret["test"] = list[3]
	    return ret


    read_iter = False
    data = []
    for line in lines:
	    line = line.strip()
	    #print line
	    if (line[:8] =='End time'):
		    break
	    if (read_iter):
		    data.append(get_info_data(line))
	    if (line=='iter \tbound \ttrain \ttest'):
		    read_iter = True

    outlines = []
    for line in data:
	    out = line['iter'] + ' ' + line['train'] + ' ' + line['test'] + ' '
	    if (showbound):
		    out += line['bound'] + ' '
	    outlines.append(out+'\n')
    f = open('error.dat', 'w')
    f.writelines(outlines)
    f.close()

    if show_separate:
	    lines = getErrors(boost_filename)
	    outlines = []
	    for line in lines:
		    out = str(line['iter']) + ' ' + str(line['neg_err']) + ' ' + str(line['pos_err']) + ' ' + str(line['err'])
		    outlines.append(out+'\n')
	    f = open('error_sep.dat', 'w')
	    f.writelines(outlines)
	    f.close()

    outlines = []

    outlines.append('set terminal png notransparent small\n')
    outlines.append('set output \'error.png\'\n')
    outlines.append('set xlabel "Iteration"\n')
    outlines.append('set ylabel "Error"\n')
    outlines.append('set title "' + info_filename + ' Error"\n')
    if show_separate:
	    outlines.append('set title "' + info_filename + ' Error (POSITIVE EXAMPLE ERROR MAY BE NEGATIVE EXAMPLE ERROR)"\n')
    if (logaxis):
	    outlines.append('set logscale x 10\n')
    outlines.append('set style line 10 lt 1 lw 1 pt 5 ps 0.65\n')
    outlines.append('set style line 11 lt 3 lw 1 pt 1 ps 0.65\n')
    outlines.append('set style line 12 lt 5 lw 1 pt 1 ps 0.65\n')
    out = ''
    if show_separate:
	    out += 'plot "error_sep.dat" using 1:2 title \'Negative Example Error\' with lines linestyle 10'
	    out += ', "error_sep.dat" using 1:3 title \'Positive Example Error\' with lines linestyle 11'
	    out += ', "error_sep.dat" using 1:4 title \'Error\' with lines linestyle 12'
    else:
	    out += 'plot "error.dat" using 1:2 title \'train\' with lines linestyle 10'
	    out += ', "error.dat" using 1:3 title \'test\' with lines linestyle 11'
	    if showbound:
		    out += ', "error.dat" using 1:4 title \'bound\' with lines'
    out += '\n'
    outlines.append(out)

    pngfilename = 'error.gnuplot'
    f = open(pngfilename, 'w')
    f.writelines(outlines)
    f.close()

    os.system('gnuplot ' + pngfilename)


if __name__ == "__main__":
    main()
