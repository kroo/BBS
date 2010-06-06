#!/usr/bin/env python

import os,sys,shutil

###degree = 0
###iterations = 10
###stump = False

testdir = 'C:\\Users\\Alex\\BBS\\balloon_images\\road_processed_data_all'
temp = os.path.join(os.path.dirname(testdir),'road_boosted_data_all_100_c')

# prepare data for jboost, then run it
for file in os.listdir(temp):

    os.chdir(temp)
    
    filepath = os.path.join(temp,file)
    
    # delete unchanged data files
    if os.path.getsize(filepath) < 1:
        os.remove(filepath)
        continue
    
    # permute data
    permutepath = os.path.join(os.path.dirname(temp),'permute_data.py')
    os.system('python %s --filename=%s'%(permutepath,filepath))
    
    # split data and limit to 750k examples each
    stem = os.path.splitext(file)[0]
    
    print 'stem = ', stem
    print 'in dir = ', os.listdir(os.getcwd())

    lines = open(stem + '.data.P').readlines()
    test = open(stem + '.test','w')
    train = open(stem + '.train','w')
    limit = min(len(lines),1500000)
    for i in range(limit):
        if i < limit/2:
            test.write(lines[i])
        else:
            train.write(lines[i])
    test.close()
    train.close()
    
    os.remove(filepath)
    os.remove(filepath + '.P')
    
    # generate spec file
    spec = open(stem + '.spec','w')
    spec.write("exampleTerminator=;\nattributeTerminator=,\nmaxBadExa=0\n")
    spec.write("labels (0,1)\nH  number\nS  number\nV  number")
    spec.close()
    
    # call jboost
    os.chdir('../../jboost-2.1')
    java_options = '-Xmx1500M'
    jboost_command = 'jboost.controller.Controller -S %s'%(os.path.join(temp,stem))
    jboost_options = '-numRounds 100' #-ATreeType ADD_ROOT
    
    score_function_name = stem + '_scoring_function'
    score_function_path = os.path.join(temp,score_function_name + '.m')
    temp_score_function_path = score_function_path + '.temp'
    c_score_function_path = os.path.join(temp,score_function_name + '.c')
    
    jboost_output = '-m %s -c %s'%(temp_score_function_path,c_score_function_path)
    print 'java %s %s %s %s'%(java_options,jboost_command,jboost_options,jboost_output)
    os.system('java %s %s %s %s'%(java_options,jboost_command,jboost_options,jboost_output))
    
    # fix function name
    input = open(temp_score_function_path)
    output = open(score_function_path,'w')
    for s in input.readlines():
        output.write(s.replace('predict',score_function_name))
    input.close()
    output.close()
    