#!/usr/bin/env python

import os,sys,shutil

boosteddir = 'C:\\Users\\Alex\\BBS\\room_scans\\boosted_data'
functiondir = 'C:\\Users\\Alex\\BBS\\room_scans\\functions_only'

for path,dirs,files in os.walk(boosteddir):
    
    # skip the first iteration
    if (len(files) == 0):
        continue
    
    print 'path: ',path
    stem = os.path.basename(path)
    os.mkdir(os.path.join(functiondir,stem))
    
    for file in files:
        
        if '.m' in file:
            input = open(os.path.join(path,file))
            output = open(os.path.join(functiondir,stem,file),'w')
            name = os.path.splitext(file)[0]
            for s in input.readlines():
                output.write(s.replace('predict',name))
            
            input.close()
            output.close()