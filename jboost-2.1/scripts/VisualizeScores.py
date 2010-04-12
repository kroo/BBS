#!/usr/bin/python

import getopt, sys, os, os.path, re, math, glob

def usage():
    print("Usage: VisualizeScores.py <info-files-path>")
    print(" <info-file-path> is a directory containing files with names")
    print(" like trial0.test.boosting.info, trial2.train.boosting.info ...")
    print("")
    print("IMPORTANT NOTE:  jboost should of been run on data and spec files passed through AddRandomIndex.py")

def main():

    carry = 0

    try:
        opts, args = getopt.getopt(sys.argv[1:],'hc',[])
    except getopt.GetoptError:
        print 'Error: Illegal arguments'
        usage()
        sys.exit(2)

    for o, a in opts:
        if o in ('-h'):
            usage()
            sys.exit()
        elif o in ('-c'):
            carry = 1
        else:
            assert False, "unhandled option"

    if len(args) < 1:
        usage()
        sys.exit(1)

    globpath = args[0]

    testfiles = glob.glob(globpath + "*.test.boosting.info")
    trainfiles = glob.glob(globpath + "*.train.boosting.info")
    infofiles = glob.glob(globpath + "*.info")
    
    for f in testfiles:
        infofiles.remove(f)
    for f in trainfiles:
        infofiles.remove(f)
    
    cmd = "java jboost.visualization.HistogramFrame " + str(carry) + " " + str(len(testfiles)) + " "
    for f in testfiles:
        cmd = cmd + f + " "
    cmd = cmd + str(len(trainfiles)) + " "
    for f in trainfiles:
        cmd = cmd + f + " "
    cmd = cmd + str(len(infofiles)) + " "
    for f in infofiles:
        cmd = cmd + f + " "
    os.system(cmd)    


    
if __name__ == "__main__":
    main()
