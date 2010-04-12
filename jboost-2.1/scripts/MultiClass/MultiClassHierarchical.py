#! /usr/bin/env python

import sys
sys.path.append('./src')
import classificationtree as ctree
import os
import getopt

def usage():
    print 'Usage: MultiClassHierarchical.py'
    print '\t--S=<stem>           path to stem name -- expecting <stem>.data and <stem>.spec to be there'
    print '\t--numRounds=N        number of rounds to run each classifier by'
    print '\t--folds=N            number of folds to use in cross-validation. Default: 5'
    print '\t--rb                 RobustBoost will be run. By default this is off.  WARNING: Can take a very long time.'
    print '\t--ATreeType=<tree>   ADD_ROOT, ADD_SINGLES, ADD_ROOT_OR_SINGLES, or ADD_ALL ADTree types (ADD_ROOT by default)'
    

def main():
    try:
        opts, args = getopt.getopt(sys.argv[1:],'' ,
                                   ['S=','numRounds=','rb','ATreeType=','folds='])
    except getopt.GetoptError:
        print 'MultiClassHierarchical.py: Illegal argument\n'
        usage()
        sys.exit(2)

    # parse options
    stem = iters = None
    RBFlag = 0
    treeType = 'ADD_ROOT'
    folds = 5
    
    for opt,arg in opts:
        if (opt == '--S'):
            stem= arg
        elif (opt == '--numRounds'):
            iters = int(arg)
        elif (opt == '--rb'):
            RBFlag = 1
        elif (opt == '--folds'):
            folds = int(arg)
        elif (opt == '--ATreeType'):
	    treeType = arg
	else: 
            print 'The argument ' + arg + ' is not a recognized option.\n'
            usage()
            sys.exit(2)
	
    if (stem == None):
        print '-S is a required option!\n'
        usage()
        sys.exit(2)
    if (iters == None):
        print '-numRounds is a required option!\n'
        usage()
        sys.exit(2)
    if (folds < 2):
        print 'folds must be a number >= 2.'
        usage()
        sys.exit(2)
        
    #Add Random Index to track examples through CV process
    os.system('/usr/bin/env python ../AddRandomIndex.py %s' % (stem))

    #Build Classification Tree Using LogLossBoost
    tree = ctree.ClassificationTree(treeType,iters,folds)
    tree.Init(stem,RBFlag)
    tree.Build()
    tree.PrintTree()
    
if __name__ == "__main__":
    main()
        
