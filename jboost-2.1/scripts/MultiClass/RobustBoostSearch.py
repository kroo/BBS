#! /usr/bin/env python

import sys
sys.path.append('./src')
import os
import getopt
import crossvalidate as cv
import spec

def usage():
    print 'Usage: RobustBoostSearch.py'
    print '\t--S=<stem>           path to stem name -- expecting <stem>.data and <stem>.spec to be there'
    print '\t--numRounds=N        maximum number of rounds of training for RobustBoost'
    print '\t--folds=N            number of folds to use in cross-validation. Default: 5'
    print '\t--ATreeType=<tree>   ADD_ROOT, ADD_SINGLES, ADD_ROOT_OR_SINGLES, or ADD_ALL ADTree types (ADD_ROOT by default)'

def CreateData(stem, classes, classIndx, zeroClassIndx):
#TODO: Fix so that this uses attributeTerminator and exampleTerminator
    f = file(stem+'_idx.data','r')
    N_0 = N_1 = 0.0
    for line in f:
        for i in range(len(classes)):
            if(line.find(',' + classes[i] + ';') >= 0):
                if(classIndx[i] in zeroClassIndx):
                    N_0 += 1.0
                else:
                    N_1 += 1.0
    f.close()

    f = file(stem+'_idx.data','r')
    write_me = []
    for line in f:
        for i in range(len(classes)):
            if(line.find(',' + classes[i] + ';') >= 0):
                if(classIndx[i] in zeroClassIndx):
                    write_me.append(line.replace(','+classes[i]+';',',%f,0;'%(1.0/N_0)))
                else:
                    write_me.append(line.replace(','+classes[i]+';',',%f,1;'%(1.0/N_1)))
    f.close()

    ovaData = './temp.data'
    f = file(ovaData,'w')
    f.writelines(write_me)
    f.close()
    
    ovaSpec = './temp.spec'
    readFile = file(stem+'_idx.spec','r')
    write_me = []
    for line in readFile:
        if(line.find('labels ') >= 0):
            write_me.append('weight number\nlabels (0,1)\n')
        else:
            write_me.append(line)
    readFile.close()
    writeFile = file(ovaSpec,'w')
    writeFile.writelines(write_me)
    writeFile.close()
    
    return ovaData,ovaSpec,N_0,N_1

def main():
    try:
        opts, args = getopt.getopt(sys.argv[1:],'' ,
                                   ['S=','numRounds=','ATreeType=','folds='])
    except getopt.GetoptError:
        print 'MultiClassHierarchical.py: Illegal argument\n'
        usage()
        sys.exit(2)

    # parse options
    stem = iters = None
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

    #Run RB search on binary problem
    ClassList,ExampleTerminator,AttributeTerminator = spec.ReadSpec(stem+'_idx.spec')
    ovaData, ovaSpec,N_0,N_1 = CreateData(stem, ClassList, range(len(ClassList)), [0])
    cv.GenerateFiles(ovaSpec,ovaData,'cv-temp',folds)
    NumExamples = int(N_0 + N_1)
    startAcc = min(N_0/NumExamples,N_1/NumExamples)/2.0
    RBAccuracy,RBAccuracyStd,RBepsilon,RBtheta,RBsigma_f,scores = cv.RunRobustSearch(treeType,iters,NumExamples,startAcc)
    print '******FINAL RESULTS*****\n'
    print 'RB: acc +/- std, (epsilon,theta,sigma_f) = %f +/- %f, (%2.2f,%2.2f,%2.2f)' % (RBAccuracy,RBAccuracyStd,RBepsilon,RBtheta,RBsigma_f)
    os.system('rm -rf cv-temp; rm temp.data; rm temp.spec');
if __name__ == "__main__":
    main()
        
