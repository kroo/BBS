#! /usr/bin/env python

import sys
sys.path.append('./src')
import crossvalidate as cv
import classificationtree as tree
import os
import spec

def CreateData(stem, name, zeroClassList):
#TODO: Fix so that this uses attributeTerminator and exampleTerminator
    f = file(stem+'_idx.data','r')
    N_0 = 0.0
    N_1 = 0.0
    for line in f:
        found = 0
        for i in range(len(zeroClassList)):
            if(line.find(',' + zeroClassList[i] + ';') >= 0):
                found = 1
                N_0 += 1.0
        if(found == 0):
            N_1 += 1.0
    f.seek(0)

    write_me = []
    for line in f:
        found = 0
        for i in range(len(zeroClassList)):
            if(line.find(',' + zeroClassList[i] + ';') >= 0):
                write_me.append(line.replace(','+zeroClassList[i]+';',',%f,0;' % (N_0/(N_1 + N_0))))
                found = 1
        if(found == 0):
            write_me.append(line[:-4] + ',%f,1;\n' % (N_1/(N_0 + N_1)))  
    f.close()
    
    ovaData = './%s-%svsAll.data' % (name, zeroClassList[0])
    f = file(ovaData,'w')
    f.writelines(write_me)
    f.close()
    
    ovaSpec = './%s-%svsAll.spec' % (name, zeroClassList[0])
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
    
    return ovaData,ovaSpec

def main():
    name = 'letter'
    stem = '../../datasets/%s/%s' % (name,name)
    RBFlag = 1
    iters = 2000
    treeType = 'ADD_ROOT_OR_SINGLES'
    NumExamples = 20000
    
    #Add Random Index to track examples through CV process
    os.system('/usr/bin/env python ../AddRandomIndex.py %s' % (stem))
    
    ClassList,ExampleTerminator,AttributeTerminator = spec.ReadSpec(stem+'_idx.spec')

    ovaData, ovaSpec = CreateData(stem, name, ['F','I','J'])
    cv.GenerateFiles(ovaSpec,ovaData,'cv-%s_ADA' % (name),5)
    cv.RunJobs('AdaBoost', treeType, iters)
    acc,std,iters = cv.GatherAccuracy()
    print 'ADA: acc +/- std, (iters) = %f +/- %f, (%d)' % (acc,std,iters)

    ovaData, ovaSpec = CreateData(stem, name, ClassList,ClassList,['F','I','J'])
    cv.GenerateFiles(ovaSpec,ovaData,'cv-%s_LLB' % (name),5)
    cv.RunJobs('LogLossBoost', treeType, iters)
    acc,std,iters = cv.GatherAccuracy()
    print 'LLB: acc +/- std, (iters) = %f +/- %f, (%d)' % (acc,std,iters)
    
    epsStart = 2*acc
    ovaData, ovaSpec = CreateData(stem, name, ClassList,ClassList,['F','I','J'])
    cv.GenerateFiles(ovaSpec,ovaData,'cv-%s_RB' % (name),5)
    acc,std,epsilon,theta,sigma_f,temp = cv.RunRobustSearch(treeType,iters,NumExamples,2*epsStart)
    print 'RB: acc +/- std, (epsilon,theta,sigma_f) = %f +/- %f, (%f,%f,%f)' % (acc,std,epsilon,theta,sigma_f)
    
if __name__ == "__main__":
    main()
        
