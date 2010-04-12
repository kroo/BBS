#!/usr/bin/python

import sys
import os.path

def usage():
    print("Usage: AddRandomIndex.py <path to data and spec file stem name>")
    print("Example: path to data and spec files are:")
    print("\t/home/jsmith/myfiles/myinfo.data and /home/jsmith/myfiles/myinfo.spec")
    print("Usage: AddRandomIndex.py /home/jsmith/myfiles/myinfo")
    
    
    
""" 
add an INDEX field to a jboost data file. INDEX is a randomly permuted number ranging
between 1 and the number of examples in the data file. The script also takes care of 
altering the spec file. This pre-processing step makes it possible to track examples through
an n-fold cross validation experiment.
"""
def main():
    
    args = sys.argv[1:]
    abort = False
    stem_path = ""
    if len(args) == 1:
        stem_path = args[0]
    else:
        abort = True
        
    datafilepath = stem_path+".data"
    specfilepath = stem_path+".spec"
 
    if not os.path.isfile(datafilepath):
        abort = True
        print("cannot find " + datafilepath)
        
    if not os.path.isfile(specfilepath):
        abort = True
        print("cannot find " + specfilepath)
        
    if abort:
        usage()
        sys.exit(2)
        
    datafile = open(datafilepath,'r')
            
    lines=[]
    morelines = datafile.readlines(100000)
    while len(morelines)>0:
        lines.extend(morelines)
        morelines = datafile.readlines(100000)
    datafile.close()
    
    length = len(lines)
    
    from random import shuffle
    shuffle(lines)
    
    newdatafilepath = stem_path+"_idx.data"
    newdatafile = open(newdatafilepath,'w')
    
    for i in range(length):
        newdatafile.write(("%d," % i)+lines[i])
        
    newdatafile.close()
    print("wrote " + newdatafilepath + ".")
          
    # add INDEX feature to top of .spec File

    spec_file = open(specfilepath,'r')
    features = spec_file.readlines()
    spec_file.close()
    
    i=0
    for idx,line in enumerate(features):
        if line.startswith(('\n','\r\n','exampleTerminator','attributeTerminator','maxBadExa','maxBadAtt')):
            i=i+1
        else:
            break
        
        
    features.insert(i, "INDEX number\n")
    
    newspecfilepath = stem_path+"_idx.spec"
    newspecfile = open(newspecfilepath,'w')
    newspecfile.write("".join(features))
    newspecfile.close()
    print("wrote " + newspecfilepath + ".")

if __name__ == "__main__":
    main()

