#! /usr/bin/env python

import os
import sys
import random
import time
import getopt
import shutil


def usage():
	print 'Usage: nfold.py --k=N  --label=STRING  --train=TRAINFILE'
	print '  --k=N             integer, times to resample integer'
	print '  --p=R             number in [0,1] prob to keep nonlabel examples'
	print '  --train=TRAINFILE the training file to oversample'
	print '  --label=STRING    the label to resample (note that this needs to be a unique identifier,'
	print '                    e.g. labels are (1,-1), then need to specify " 1" or ",1")'


# taken from http://aspn.activestate.com/ASPN/Python/Cookbook/Recipe/52306
# a further slight speed-up on my box
# is to map a bound-method:
def sort_dict(d, reverse=False):
	keys = d.keys()
	vals = d.keys()
	if (reverse):
		keys.sort(lambda x,y : y-x)
	else:
		keys.sort(lambda x,y : x-y)		
	return [(key,d[key]) for key in keys]

def generateFile(trainfile, oversample, undersample, labelstr):
	f= file(trainfile, 'r')
	data= f.readlines()
	f.close()


	add_lines = {}
	num_positive_examples = 0
	i = 0
	for line in data:
		end = len(line)
		if line[end-4:end-1].rstrip() == ' 1;':
			add_lines[i] = line
			num_positive_examples += 1
		elif line[end-4:end-1].rstrip() == '-1;':
			# do nothing
			x = 0
		else:
			print 'You clearly think this script was well written... you are mistaken...'
			usage()
			sys.exit(2)
		i = i + 1

	add_lines = sort_dict(add_lines)
	for (key, val) in add_lines:
		for j in range(oversample):
			data.insert(key,val)


	del_lines = {}
	num_negative_examples = 0
	i = 0
	for line in data:
		end = len(line)
		if line[end-4:end-1].rstrip() == ' 1;':
			# do nothing
			x = 0
		elif line[end-4:end-1].rstrip() == '-1;':
			del_lines[i] = 'd'
			num_negative_examples += 1
		else:
			print 'You clearly think this script was well written... you are mistaken...'
			usage()
			sys.exit(2)
		i = i + 1

	del_lines = sort_dict(del_lines, True)
	random.seed(107)
	num_lines_deleted = 0
	for (key, val) in del_lines:
		r = random.random()
		if r > undersample:
			num_lines_deleted += 1;
			data.pop(key)

	print 'Original number of examples:'
	print '\tNegative examples:',  num_negative_examples
	print '\tPositive examples:',  num_positive_examples
	print 'After sampling:'
	print '\tNegative examples:',  num_negative_examples - num_lines_deleted
	print '\tPositive examples:',  num_positive_examples * (oversample + 1)
	print 'Expected num lines deleted:',  num_negative_examples  * (1 - undersample)
	print 'Actual num lines deleted:', num_lines_deleted
			
	trainsuffix= '.train'
	oversample = oversample + 1
	tfilename = trainfile + '.' + str(oversample) + '.' + str(undersample) + trainsuffix
	tfile= file(tfilename, 'w')
	tfile.writelines(data)
	tfile.close()
	os.system('mv ' + tfilename + ' ' + trainfile)
	
    
def main():
    try:
        opts, args = getopt.getopt(sys.argv[1:],'' ,
                                   ['k=','train=','label=','p='])
    except getopt.GetoptError:
        print 'resample.py: Illegal argument\n'
        usage()
        sys.exit(2)

    # parse options
    trainfile = k = p = label = None
    for opt,arg in opts:
	    if (opt == '--train'):
		    trainfile = arg
	    elif (opt == '--k'):
		    k = int(arg)
	    elif (opt == '--p'):
		    p = float(arg)
	    elif (opt == '--label'):
		    label = arg
	    else:
		    usage()
		    sys.exit(2)


    if trainfile==None:
	    print 'Must specify file for resampling'
	    usage()
	    sys.exit(2)

    if k==None and p==None:
	    print 'Must specify over/under sample quanitity'
	    usage()
	    sys.exit(2)

    if not k==None and k < 2:
	    print 'k must be larger than 2 for anything to happen'
	    sys.exit(2)

    if not p==None and p > 1:
	    print 'p  must be less than 1 for anything to happen'
	    sys.exit(2)

    if p==None:
	    p = 1
    if k==None:
	    k = 1

    k = int(k) - 1
    print 'Oversample by', k
    print 'Prob sample', p
    generateFile(trainfile, k, p, label)





if __name__ == "__main__":
    main()
