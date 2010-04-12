# Generates files in a specified directory for n-fold crossvalidation
# Evan Ettinger 4/16/09

import shutil
import random
import math
import os
import re
import numpy as np

gFolds = 10
gDirName = 'temp'
gRounds = 0

################################################################
# specfile    - path to specfile
# datafile    - path to file containing data
# dirname     - name of the directory to be created for the CV files
# K           - number of folds to create
#
# Creates K CV folds in the directory dirname randomly generated
# from the data in datafile.  Fold <i> will be called
# trial<i>.train and trial<i>.test.  The spec file is copied over
# as trial.spec.
def GenerateFiles(specFile, dataFile, dirName, K):
    global gFolds, gDirName
    gFolds = K
    gDirName = dirName
    try:
        os.mkdir('./%s' % (dirName))
    except OSError:
        print 'Directory %s already exists!' % (dirName)
    
    f = file(dataFile,'r')
    data = f.readlines()
    f.close()
    random.seed(42)
    random.shuffle(data)

    if(len(data) < gFolds):
        K = len(data)
        gFolds = len(data)

    train_writeme = [[''] for i in range(K)]
    test_writeme = [[''] for i in range(K)]
    for i in range(len(data)):
        test_fold = math.floor(float(i)/float(len(data))*float(K)) + 1
        for j in range(1,K+1):
            if(j == test_fold):
                test_writeme[j-1].append(data[i])
            else:
                train_writeme[j-1].append(data[i])

    for i in range(1,K+1):
        writefile = file('./%s/trial%d.test' % (dirName,i), 'w')
        writefile.writelines(test_writeme[i-1][:])
        writefile.close()
        writefile = file('./%s/trial%d.train' % (dirName,i), 'w')
        writefile.writelines(train_writeme[i-1][:])
        writefile.close()
        
    shutil.copy(specFile, './%s/trial.spec' % (dirName))
    
################################################################
# boostType - type of Booster e.g. AdaBoost, RobustBoost
# treeType  - type of ADTree e.g. ADD_ROOT, ADD_ALL
# rounds    - number of rounds to run N
def RunJobs(boostType, treeType, rounds):
    global gFolds, gDirName,gRounds
    gRounds = rounds
    os.chdir(gDirName)
    for i in range(1,gFolds+1):
        command = 'java -Xmx1G -cp ../../../lib/concurrent.jar:'\
                  + '../../../dist/jboost.jar jboost.controller.Controller'\
                  + ' -b %s -numRounds %d' % (boostType,rounds)\
                  + ' -p 25 -S trial%d' % (i) \
                  + ' -n trial.spec -ATreeType %s' % (treeType)\
                  + ' -a -3 -loglevel 2'
        SubmitJob(command)
    os.chdir('../')

def GetRBInfo():
    global gFolds, gDirName,gRounds
    os.chdir(gDirName)
    test_error = np.array(np.ones((gFolds)))
    time = np.array(np.zeros((gFolds)))
    for i in range(1,gFolds+1):
        f = file('./trial%d.info' % i,'r')
        startFlag = 0
        for line in f:
            if(line.find('End time=') >=0):
                break
            if(startFlag > 0):
                matched = re.findall('[0-9.-]+',line)
                if(matched == None):
                    break
                test_error[i-1] = float(matched[3])
                time[i-1] = float(matched[4])
                if(time[i-1] > 1.0):
                    time[i-1] = 1.0
                startFlag += 1
            elif(line.find('iter') >=0 and line.find('train') >=0 and line.find('test') >=0):
                startFlag = 1
    os.chdir('../')
    return time.mean(),test_error.mean(),test_error.std()

    

def RunRBJob(treeType,rounds,eps,theta,sigma_f):
    global gFolds, gDirName,gRounds

    def GetTVal(i):
        global gFolds, gDirName,gRounds
        time = 0
        f = file('./trial%d.info' % (i),'r')
        startFlag = 0
        for line in f:
            if(line.find('End time=') >=0):
                break
            if(startFlag > 0):
                matched = re.findall('[0-9.-]+',line)
                if(matched == None):
                    break
                time = float(matched[4])
                if(time > 1.0):
                    time = 1.0
                startFlag += 1
            elif(line.find('iter') >=0 and line.find('train') >=0 and line.find('test') >=0):
                startFlag = 1
        return time

    gRounds = rounds
    os.chdir(gDirName)
    for i in range(1,gFolds+1):
        command = 'java -Xmx1G -cp ../../../lib/concurrent.jar:'\
                  + '../../../dist/jboost.jar jboost.controller.Controller'\
                  + ' -b RobustBoost -numRounds %d' % (rounds)\
                  + ' -p 25 -S trial%d' % (i) \
                  + ' -n trial.spec -ATreeType %s' % (treeType)\
                  + ' -a -3 -loglevel 2' \
                  + ' -rb_epsilon %f -rb_theta %f -rb_sigma_f %f' % (eps,theta,sigma_f)
        SubmitJob(command)
        if(GetTVal(i) < 1.0):
            os.chdir('../')
            return 0,1,0
    os.chdir('../')
    t_val,acc,std = GetRBInfo()
    return t_val,acc,std

#Given a theta, finds the smallest epsilon that lets RB
# get to t=1.0.
def FindBestEpsilon(treeType,rounds,theta,sigma_f,N,eps_top):
    #Do a binary search until you get within .01 of the best epsilon
    scores = None
    eps_TOL = 0.01
    low_eps = 0.0
    high_eps = eps_top
    t_thresh = 0.95
    if(high_eps == 0.0):
        high_eps = 5.0/float(N)

    t_val, acc, std = RunRBJob(treeType,rounds,low_eps,theta,sigma_f)
    if(t_val >= t_thresh):
        scores = GatherScores(N)
        return low_eps,acc,std,scores

    t_val, retAcc, retStd = RunRBJob(treeType,rounds,high_eps,theta,sigma_f)
    if(t_val < t_thresh):
        return high_eps,retAcc,retStd,scores

    while(high_eps - low_eps > eps_TOL):
        t_val, acc, std = RunRBJob(treeType,rounds,low_eps + (high_eps - low_eps)/2.0,theta,sigma_f)
        if(t_val >= t_thresh):
            scores = GatherScores(N)
            retAcc = acc
            retStd = std
            high_eps = low_eps + (high_eps - low_eps)/2.0
        else:
            low_eps = low_eps + (high_eps - low_eps)/2.0
        
    return high_eps,retAcc,retStd,scores

#Runs the RB search for the best epsilon/theta combo
def RunRobustSearch(treeType,rounds,N,eps_top):
    thetas = [0,1,2,4,8,16,32,64]
    acc = [1]*len(thetas)
    stds = [0]*len(thetas)
    sigma_f = 1.0
    epsilons = [1]*len(thetas)
    scores = np.ndarray(((len(thetas),N)))
    bestAcc = 1.0
    bestIndex = 0
    for i in range(len(thetas)):
        epsilons[i],acc[i],stds[i],temp = FindBestEpsilon(treeType,rounds,thetas[i],sigma_f,N,eps_top)
        if(temp == None):
            epsilons[i],acc[i],stds[i],temp = FindBestEpsilon(treeType,rounds,thetas[i],sigma_f,N,2*eps_top)
            if(temp == None):
                break
        scores[i,:] = temp
        if(acc[i] < bestAcc):
            bestAcc = acc[i]
            bestIndex = i
    return acc[bestIndex],stds[bestIndex],epsilons[bestIndex],thetas[bestIndex],sigma_f,scores[bestIndex,:]

    
    

################################################################
# Output the accuracy and std for this run for all test examples
def GatherAccuracy():
    global gFolds, gDirName,gRounds
    os.chdir(gDirName)
    test_error = np.array(np.ones((gRounds+1,gFolds)))
    indx = gRounds + 1
    for i in range(1,gFolds+1):
        f = file('./trial%d.info' % i,'r')
        startFlag = 0
        for line in f:
            if(line.find('End time=') >=0):
                break
            if(startFlag > 0):
                matched = re.findall('[0-9.-]+',line)
                if(matched == None):
                    break
                test_error[startFlag - 1,i-1] = float(matched[3])
                startFlag += 1
            elif(line.find('iter') >=0 and line.find('train') >=0 and line.find('test') >=0):
                startFlag = 1
        if(startFlag - 2 < indx):
            indx = startFlag - 2
    os.chdir('../')
    return test_error[indx, :].mean(),test_error[indx, :].std(),indx
#     bestError = bestStd = bestIter = 2.0
#     for i in range(startFlag -1):
#         mean_error = test_error[i,:].mean()
#         if(mean_error < bestError):
#             bestError = mean_error
#             bestIter = i
#             bestStd = test_error[i,:].std()

#     return bestError,bestStd,bestIter
            

################################################################
# N - number of training examples
#
# Output the boosting scores for this run for all test examples
def GatherScores(N):
    global gFolds, gDirName,gRounds
    os.chdir(gDirName)

    scores = [0]*N    
    for i in range(1,gFolds+1):
        f = file('./trial%d.test.boosting.info'%(i),'r')
        startFlag = 0
        for line in f:
            if(startFlag):
                splitter = line.split(':')
                scores[int(splitter[1].strip())] = float(splitter[3].strip())
            else:
                if(line.startswith('iteration=')):
                    startFlag = 1
                
    os.chdir('../')
    return scores

################################################################
# jobStr - string of what unix job to sumbit
#
# Change me to submit jobs to a grid
def SubmitJob(jobstr):
    print '**********Executing command:\n'+jobstr
    os.system(jobstr)

################################################################
# Removes dirname after were done with it
def CleanUp():
    global gDirName
    os.system('rm -rf ./' + gDirName)

