#! /usr/bin/env python

import os
import sys
import random
import time
import getopt
import shutil
import glob


def usage():
	print 'Usage: nfold.py <--booster=boosttype> <--folds=number> [--generate | --dir=dir] [--data=file --spec=file] [--rounds=number --tree=treetype]'
	print '\t--booster=TYPE     AdaBoost, LogLossBoost, BrownBoost, etc (required)'
	print '\t--folds=N          create N files for N fold cross validation (required)'
	print '\t--data=DATAFILE    DATAFILE contains the data to be divided into test/train sets (required)'
	print '\t--spec=SPECFILE    SPECFILE is the same as used in JBoost typically (required)'
	print '\t--rounds=N         The number of rounds to run the booster (required)'
	print '\t--tree=TREETYPE    The type of tree nodes to use possible options are ADD_ROOT, '
	print '\t                   ADD_ALL, ADD_SINGLES, ADD_ROOT_OR_SINGLES.  If unspecified, all'
	print '\t                   types will be executed sequentially'
	print '\t--generate         Creates directories for the cross validation sets'
	print '\t--dir=DIR          Specifies a name for the directory to be generated'

    
    
def learner(atreeoption, k, rounds, booster):
    # XXX: put in description!
    config= os.getenv('JBOOST_CONFIG')
    command = 'java -Xmx1000M -cp ' + os.getenv('CLASSPATH') \
	      + ' jboost.controller.Controller -b ' + booster + ' -p 3 -a -1 -S trial' + str(k) \
	      + ' -n trial.spec -ATreeType '+ atreeoption +' -numRounds ' + str(rounds)
    if (config != None):
	    command = command + ' -CONFIG ' + config
    
    print command
    error= os.system(command)
    if (error != 0):
        sys.exit(1)
  
  
    
def moveresults(foldername):
# XXX: put in description!

    for f in glob.glob('*.tree'):
    	shutil.move(f,foldername)
	for f in glob.glob('*.info'):
		shutil.move(f,foldername)
	for f in glob.glob('*.scores'):
		shutil.move(f,foldername)
	for f in glob.glob('*.output.tree'):
		shutil.move(f,foldername)
	for f in glob.glob('*.sampling'):
		shutil.move(f,foldername)
	for f in glob.glob('*.log'):
		shutil.move(f,foldername)
			
#	os.system('mv *.tree ./'+foldername)   	    
#    os.system('mv *.info ./'+foldername)    
#    os.system('mv *.scores ./'+foldername)
#    os.system('mv *.output.tree ./'+foldername)
#    os.system('mv *.sampling ./'+foldername)
#    os.system('mv *.log ./'+foldername)



def generateFiles(datafile, fileprefix, folds):
    # XXX: put in description
    # load data and shuffle it
    f= file(datafile, 'r')
    data= f.readlines()
    f.close()
    random.seed()
    random.shuffle(data)
    
    testsuffix= '.test'
    trainsuffix= '.train'
    
    # split data into n folds, and output test/train files
    # run learner on each pair
    for k in range(folds):
        start= (len(data)/folds)*k
        end = start + len(data)/folds
        print 'k: ' + str(k) + ' start:' + str(start) +' end:' + str(end)
        # create test/train files
	trainfilename = fileprefix + str(k) + trainsuffix;
	testfilename = fileprefix + str(k) + testsuffix;
        testfile= file(fileprefix + str(k)+ testsuffix, 'w')
        trainfile= file(fileprefix + str(k) + trainsuffix, 'w')
        testfile.writelines(data[start:end])
        trainfile.writelines(data[:start])
        trainfile.writelines(data[end:])
        testfile.close()
        trainfile.close()
	#os.system('./resample.py --k=128 --label=" 1" --train=' + trainfilename);

    
def main():
# Usage:
#   nfold.py <--folds=number> <--data=datafile> <--spec=specfile> <--rounds> [--dir=dir] [--generate] [--treetype=type]
#
# Uses folds to determine how many iterations to run. 
# Loads data file, randomly shuffles data and then outputs into a number of testing and training files. 
# Finally, calls a learning function to generate results


    try:
        opts, args = getopt.getopt(sys.argv[1:],'' ,
                                   ['booster=','folds=','data=','spec=','dir=','rounds=','tree=','generate'])
    except getopt.GetoptError:
        print 'nfold.py: Illegal argument\n'
        usage()
        sys.exit(2)

    # parse options
    booster = datafile = specfile = folds = dirname = generateData = rounds = tree = None
    for opt,arg in opts:
        if (opt == '--data'):
            datafile= arg
        elif (opt == '--spec'):
            specfile= arg
        elif (opt == '--booster'):
            booster= arg
        elif (opt == '--folds'):
            folds = int(arg)
        elif (opt == '--dir'):
            dirname= arg
        elif (opt == '--generate'):
            generateData= 1
        elif (opt == '--rounds'):
            rounds= int(arg)
        elif (opt == '--tree'):
	    tree= arg
	else:
#		print 'The argument ' + arg + ' is not a recognized option.\n'
		usage()
		sys.exit(2)
	
    if (rounds == None):
        rounds = 20
	
    if (folds == None):
	    print 'nfold.py: --folds is a required parameter.\n'
	    usage()
	    sys.exit(2)
	    
    if (booster == None):
	    print 'nfold.py: --booster is a required parameter.\n'
	    usage()
	    sys.exit(2)
	    
    if (datafile != None and specfile == None):
	    print 'nfold.py: --data option requies --spec option.\n'
	    usage()
	    sys.exit(2)

    if (dirname == None and generateData == None):
	    print 'nfold.py: Either --dir or --generate must be specified.\n'
	    usage()
	    sys.exit(2)
	    
    if (generateData != None and dirname == None and (specfile == None or datafile == None)):
	    print 'nfold.py: --generate requires either a --dir or the --spec and --data options.\n'
	    usage()
	    sys.exit(2)

    if os.getenv('CLASSPATH') == None:
	    print 'nfold.py: Your CLASSPATH is not set. You must place jboost.jar in your CLASSPATH.\n'
	    sys.exit(1)

	    

    # if we set the generate data flag, then we will create the
    # destination directory and populate with random folds of datafile
    if (generateData != None):
        # create directory for n-folds of data
        if (dirname == None):
            dirname= datafile + ".folds_" + str(folds)

        try:
            os.mkdir(dirname)
        except OSError:
            print 'Directory '+ dirname + ' already exists.'
        
        # copy specfile into new directory
        shutil.copyfile(specfile,
                        os.path.join(os.getcwd(),dirname,'trial.spec'))
        
        # move into the new directory
        fileprefix= os.path.join(os.getcwd(),dirname,'trial')
        generateFiles(datafile,fileprefix, folds)


    # after we check to see if we need to generate the data
    # it is time to run learner on data

    os.chdir(dirname)
    
    # for each atree option, create a directory
    # run the learner for all splits, move results to the new dir
    if (tree == None):
	    adtrees=['ADD_ROOT','ADD_ALL', 'ADD_SINGLES', 'ADD_ROOT_OR_SINGLES']
    else:
	    adtrees=[tree]
	
    treedir_prefix = "cvdata-" + time.strftime("%m-%d-%y-%H-%M-%S")
    try:
        os.mkdir(treedir_prefix)
    except OSError:
        print 'Directory '+ treedir_prefix + ' already exists.'
	
    for treetype in adtrees:
        try:
            os.mkdir(os.path.join(treedir_prefix,treetype))
        except OSError:
#            print 'Directory '+ treetype + ' already exists.'

            print
            print 'Running ' + str(folds) + ' learning experiments for ' + treetype + ' tree type.'
        for k in range(folds):
            print '*=---------------------------------------------------------------------=-*'
            print '* Fold ' + str(k) + '     |'
            print '*============'
            learner(treetype, k, rounds, booster)
            print '*=---------------------------------------------------------------------=-*'
            moveresults(os.path.join(treedir_prefix,treetype))
        




if __name__ == "__main__":
    main()
