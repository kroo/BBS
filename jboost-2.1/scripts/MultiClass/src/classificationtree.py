# Makes a classification tree object
# Evan Ettinger 4/20/09

import sys
import shutil
import os
import spec
try:
    import numpy as np
except ImportError:
    print 'Import error: numpy module is required to run this script!'
    sys.exit(2)
import crossvalidate as cv
import spectralclustering as sc

class Node:
    def __init__(self):
        self.ConfusionMatrix = None
        self.ClassList = []
        self.OneLabels = []
        self.ZeroLabels = []
        self.LLBAccuracy = 0.0
        self.LLBAccuracyStd = 0.0
        self.LLBIters = 0
        self.ADAAccuracy = 0.0
        self.ADAAccuracyStd = 0.0
        self.ADAIters = 0
        self.ADAConfusionMatrix = None
        self.RBConfusionMatrix = None
        self.RBAccuracy = 0.0
        self.RBAccuracyStd = 0.0
        self.RBepsilon = 0
        self.RBtheta = 0
        self.RBsigma_f = 0
        self.pLeft = None
        self.pRight = None
        self.NumExamples = 0
        
class ClassificationTree:
    def __init__(self,treeType,rounds,folds):
        self.Root = None
        self.Stem = ''
        self.ExampleTerminator = ''
        self.AttributeTerminator = ''
        self.TrueLabels = []
        self.NumExamples = 0
        self.ClassList = []
        self.TreeType = treeType
        self.Rounds = rounds
        self.LLBOneVsAllAccuracy = 0
        self.LLBTreeIncorrects = []
        self.ADATreeIncorrects = []
        self.RBTreeIncorrects = []
        self.RunRB = 0
        self.EpsStart = 0.0
        self.Folds = folds
        
    def Init(self, stem, rbFlag):
        self.RunRB = rbFlag
        self.Stem = stem
        self.ClassList,self.ExampleTerminator,self.AttributeTerminator = spec.ReadSpec(stem+'_idx.spec')
        self.NumExamples, self.TrueLabels = GetExamplesInfo(stem+'_idx.data',self.ClassList,self.ExampleTerminator,self.AttributeTerminator)

    def Build(self):
        self.Root = Node()
        self.Root.ClassList = range(len(self.ClassList))
        self.Root.NumExamples = self.NumExamples
        LearnOnevsAllADA(self,self.Root)
        if(self.RunRB):
            LearnOnevsAllRB(self,self.Root)
        RecursiveBuildNode(self,self.Root)
        
    def PrintTree(self):
        RecursivePrintNode(self,self.Root,0)
        LLBOneVsAllAccuracy = 1.0 - float(np.diag(self.Root.ConfusionMatrix).sum())/float(self.Root.ConfusionMatrix.sum())
        ADAOneVsAllAccuracy = 1.0 - float(np.diag(self.Root.ADAConfusionMatrix).sum())/float(self.Root.ADAConfusionMatrix.sum())
        if(self.RunRB):
            RBOneVsAllAccuracy = 1.0 - float(np.diag(self.Root.RBConfusionMatrix).sum())/float(self.Root.RBConfusionMatrix.sum())
        print '\n************************SUMMARY************************'
        print 'LLB 1 vs All Classification Error:               %2.4f' % (LLBOneVsAllAccuracy)
        print 'ADA 1 vs All Classification Error:               %2.4f' % (ADAOneVsAllAccuracy)
        if(self.RunRB):
            print 'RB 1 vs All Classification Error:                %2.4f' % (RBOneVsAllAccuracy)
        print 'LLB Spectral Cluster Tree Classification Error:  %2.4f' % (float(len(self.LLBTreeIncorrects))/float(self.NumExamples))
        print 'ADA Spectral Cluster Tree Classification Error:  %2.4f' % (float(len(self.ADATreeIncorrects))/float(self.NumExamples))
        if(self.RunRB):
            print 'RB Spectral Cluster Tree Classification Error:   %2.4f' % (float(len(self.RBTreeIncorrects))/float(self.NumExamples))

def RecursivePrintNode(tree,node,indent):
#TODO: Print the tree out into a nice HTML page + directory structure
    spacer = '   '*indent
    print spacer + '*********New Node**********'
    print spacer + 'ClassList:\n', spacer, [tree.ClassList[i] for i in node.ClassList]
    print spacer + 'Confusion Matrix:'

    def GetNumDigits(n):
        if n == 0: return 1
        numDigits = 0
        while(n > 0):
            numDigits += 1
            n /= 10
        return numDigits
    
    digitSpacer = GetNumDigits(node.ConfusionMatrix.max()) + 3
    for i in range(node.ConfusionMatrix.shape[0]):
        printMe = spacer + ' '*(digitSpacer - GetNumDigits(node.ConfusionMatrix[i,0]) - 2)
        for j in range(node.ConfusionMatrix.shape[1]-1):
            printMe += str(node.ConfusionMatrix[i,j]) + ' '*(digitSpacer - GetNumDigits(node.ConfusionMatrix[i,j+1]))
        print printMe + str(node.ConfusionMatrix[i,node.ConfusionMatrix.shape[1]-1])
    print spacer + 'ZeroLabels:\n', spacer, [tree.ClassList[i] for i in node.ZeroLabels]
    print spacer + 'OneLabels:\n', spacer, [tree.ClassList[i] for i in node.OneLabels]
    print spacer + 'LLB: acc +/- std, (iters) = %f +/- %f, (%d)' % (node.LLBAccuracy,node.LLBAccuracyStd,node.LLBIters)
    print spacer + 'ADA: acc +/- std, (iters) = %f +/- %f, (%d)' % (node.ADAAccuracy,node.ADAAccuracyStd,node.ADAIters)
    if(tree.RunRB):
        print spacer + 'RB: acc +/- std, (epsilon,theta,sigma_f) = %f +/- %f, (%2.2f,%2.2f,%2.2f)' % (node.RBAccuracy,node.RBAccuracyStd,node.RBepsilon,node.RBtheta,node.RBsigma_f)
    if(node.pLeft != None):
        RecursivePrintNode(tree,node.pLeft,indent + 1)
    if(node.pRight != None):
        RecursivePrintNode(tree,node.pRight,indent + 1)

def GetExamplesInfo(data,classlist,exampleTerminator,attributeTerminator):
    f = file(data,'r')
    trueLabels = []
    lenData = 0
    for line in f:
        line = line.replace(exampleTerminator,'')
        splitter = line.split(attributeTerminator)
        trueLabels.append(classlist.index(splitter[-1].strip()))
        lenData += 1
    f.close()
    return lenData,trueLabels

def CreateData(stem, name, classes, classIndx, zeroClassIndx):
#TODO: Fix so that this uses attributeTerminator and exampleTerminator
    f = file(stem+'_idx.data','r')
    write_me = []
    for line in f:
        for i in range(len(classes)):
            if(line.find(',' + classes[i] + ';') >= 0):
                if(classIndx[i] in zeroClassIndx):
                    write_me.append(line.replace(','+classes[i]+';',',0;'))
                else:
                    write_me.append(line.replace(','+classes[i]+';',',1;'))
    f.close()
    ovaData = './%s-%dvsAll.data' % (name, zeroClassIndx[0])
    f = file(ovaData,'w')
    f.writelines(write_me)
    f.close()
    
    ovaSpec = './%s-%dvsAll.spec' % (name, zeroClassIndx[0])
    readFile = file(stem+'_idx.spec','r')
    write_me = []
    for line in readFile:
        if(line.find('labels ') >= 0):
            write_me.append('labels (0,1)\n')
        else:
            write_me.append(line)
    readFile.close()
    writeFile = file(ovaSpec,'w')
    writeFile.writelines(write_me)
    writeFile.close()
    
    return ovaData,ovaSpec

def LearnOnevsAllADA(tree,node):
    #Get 1 vs All boosting score matrix
    name = tree.Stem.split('/')[-1].strip()
    numClasses = len(node.ClassList)
    scores = np.ndarray((numClasses,tree.NumExamples),dtype=float)
    classList = [tree.ClassList[i] for i in node.ClassList]
    for i in range(numClasses):
        ovaData, ovaSpec = CreateData(tree.Stem, name, classList,node.ClassList,[node.ClassList[i]])
        cv.GenerateFiles(ovaSpec,ovaData,'cv-%s-%d' % (name,i),tree.Folds)
        cv.RunJobs('AdaBoost', tree.TreeType, tree.Rounds)
        scores[i,:] = cv.GatherScores(tree.NumExamples)
        acc,std,iters = cv.GatherAccuracy()
        if(acc > tree.EpsStart):
            tree.EpsStart = acc
        cv.CleanUp()
        os.remove(ovaData)
        os.remove(ovaSpec)

    #Get Confusion Matrix
    CM = np.mat([[0]*numClasses for i in range(numClasses)])
    for i in range(tree.NumExamples):
        if(tree.TrueLabels[i] in node.ClassList):
            idx = node.ClassList.index(tree.TrueLabels[i])
            CM[idx,scores[:,i].argmax()] += 1
    tree.Root.ADAConfusionMatrix = CM

def LearnOnevsAllRB(tree,node):
    #Get 1 vs All boosting score matrix
    name = tree.Stem.split('/')[-1].strip()
    numClasses = len(node.ClassList)
    scores = np.ndarray((numClasses,tree.NumExamples),dtype=float)
    classList = [tree.ClassList[i] for i in node.ClassList]
    for i in range(numClasses):
        ovaData, ovaSpec = CreateData(tree.Stem, name, classList,node.ClassList,[node.ClassList[i]])
        cv.GenerateFiles(ovaSpec,ovaData,'cv-%s-%d' % (name,i),tree.Folds)
        acc,std,epsilon,theta,sigma_f,temp = cv.RunRobustSearch(tree.TreeType,tree.Rounds,tree.NumExamples,2*tree.EpsStart)
        scores[i,:] = temp
        cv.CleanUp()
        os.remove(ovaData)
        os.remove(ovaSpec)

    #Get Confusion Matrix
    CM = np.mat([[0]*numClasses for i in range(numClasses)])
    for i in range(tree.NumExamples):
        if(tree.TrueLabels[i] in node.ClassList):
            idx = node.ClassList.index(tree.TrueLabels[i])
            CM[idx,scores[:,i].argmax()] += 1
    tree.Root.RBConfusionMatrix = CM

def RecursiveBuildNode(tree,node):
    #Get 1 vs All boosting score matrix
    name = tree.Stem.split('/')[-1].strip()
    numClasses = len(node.ClassList)
    CM = np.mat([[0]*numClasses for i in range(numClasses)])
    classList = [tree.ClassList[i] for i in node.ClassList]
    if(numClasses > 2):
        scores = np.ndarray((numClasses,tree.NumExamples),dtype=float)
        for i in range(numClasses):
            ovaData, ovaSpec = CreateData(tree.Stem, name, classList,node.ClassList,[node.ClassList[i]])
            cv.GenerateFiles(ovaSpec,ovaData,'cv-%s-%d' % (name,i),tree.Folds)
            cv.RunJobs('LogLossBoost', tree.TreeType, tree.Rounds)
            scores[i,:] = cv.GatherScores(tree.NumExamples)
            cv.CleanUp()
            os.remove(ovaData)
            os.remove(ovaSpec)

        #Get Confusion Matrix
        for i in range(tree.NumExamples):
            if(tree.TrueLabels[i] in node.ClassList):
                idx = node.ClassList.index(tree.TrueLabels[i])
                CM[idx,scores[:,i].argmax()] += 1

        node.NumExamples = CM.sum()

        #Do spectral clustering into 2 groups and figure out the
        # group that will have label '0' and the remaining has '1'
        groups = sc.DoSpectralClustering(CM,2)
        zeroCount = 0
        for i in range(len(groups)):
            if(groups[i] == 0):
                node.ZeroLabels.append(node.ClassList[i])
                #Swap Rows
                temp = CM[i,:].copy()
                CM[i,:] = CM[zeroCount,:]
                CM[zeroCount,:] = temp
                #Swap Cols
                temp = CM[:,i].copy()
                CM[:,i] = CM[:,zeroCount]
                CM[:,zeroCount] = temp
                zeroCount += 1
            else:
                node.OneLabels.append(node.ClassList[i])
        node.ConfusionMatrix = CM
    else:
        node.ZeroLabels.append(node.ClassList[0])
        node.OneLabels.append(node.ClassList[1])
    
    #Create the classifier for 0 vs 1
    ovaData, ovaSpec = CreateData(tree.Stem, name, classList,node.ClassList,node.ZeroLabels)
    node.ClassList = []
    for i in node.ZeroLabels: node.ClassList.append(i)
    for i in node.OneLabels: node.ClassList.append(i)

    #Run LLB and gather
    cv.GenerateFiles(ovaSpec,ovaData,'cv-%s-%d' % (name,i),tree.Folds)
    cv.RunJobs('LogLossBoost', tree.TreeType, tree.Rounds)
    temp = cv.GatherScores(tree.NumExamples)
    node.LLBAccuracy,node.LLBAccuracyStd,node.LLBIters = cv.GatherAccuracy()
    for i in range(len(temp)):
        if(tree.TrueLabels[i] in node.ClassList):
            if(numClasses == 2):
                predictIndx = 0
                if(temp[i] < 0):
                    predictIndx = 1
                trueIndx = 0
                if(tree.TrueLabels[i] in node.OneLabels):
                    trueIndx = 1
                CM[trueIndx,predictIndx] += 1
            if((temp[i] < 0 and tree.TrueLabels[i] in node.ZeroLabels) or \
               (temp[i] >=0 and tree.TrueLabels[i] in node.OneLabels)):
                if(not i in tree.LLBTreeIncorrects):
                    tree.LLBTreeIncorrects.append(i)

    if(numClasses == 2):
        node.ConfusionMatrix = CM
        node.NumExamples = CM.sum()
        
    #Run ADA and gather
    cv.RunJobs('AdaBoost', tree.TreeType, tree.Rounds)
    temp = cv.GatherScores(tree.NumExamples)
    node.ADAAccuracy,node.ADAAccuracyStd,node.ADAIters = cv.GatherAccuracy()
    for i in range(len(temp)):
        if(tree.TrueLabels[i] in node.ClassList):
            if((temp[i] < 0 and tree.TrueLabels[i] in node.ZeroLabels) or \
               (temp[i] >=0 and tree.TrueLabels[i] in node.OneLabels)):
                if(not i in tree.ADATreeIncorrects):
                    tree.ADATreeIncorrects.append(i)

    if(tree.RunRB):
        node.RBAccuracy,node.RBAccuracyStd,node.RBepsilon,node.RBtheta,node.RBsigma_f,temp = cv.RunRobustSearch(tree.TreeType,tree.Rounds,tree.NumExamples,node.LLBAccuracy)
        for i in range(len(temp)):
            if(tree.TrueLabels[i] in node.ClassList):
                if((temp[i] < 0 and tree.TrueLabels[i] in node.ZeroLabels) or \
                   (temp[i] >=0 and tree.TrueLabels[i] in node.OneLabels)):
                    if(not i in tree.RBTreeIncorrects):
                        tree.RBTreeIncorrects.append(i)
    
    #Clean up
    cv.CleanUp()
    os.remove(ovaData)
    os.remove(ovaSpec)

    #Recurse
    if(len(node.ZeroLabels) > 1):
        node.pLeft = Node()
        node.pLeft.ClassList = node.ZeroLabels
        RecursiveBuildNode(tree,node.pLeft)
    if(len(node.OneLabels) > 1):
        node.pRight = Node()
        node.pRight.ClassList = node.OneLabels
        RecursiveBuildNode(tree,node.pRight)
