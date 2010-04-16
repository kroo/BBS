#!/usr/bin/env python

import sys, os

# check for arguments
if len(sys.argv) == 1:
    print 'Usage: Pass in an image and an xml file'
    print 'Run from same directory as passed files'
    sys.exit()

# directory management
scripts = sys.path[0]   # full path to scripts
dir = os.getcwd()       # full path to working directory
title = os.path.split(dir)[1]   # short name of working directory
temp = dir + '/' + title + '_temp'    # full path to temp directory
os.mkdir(temp)

# prepare MATLAB input
image = sys.argv[1]
xml = sys.argv[2]
data = temp + '/' + title + '.data'

# run MATLAB script to prepare data
### do I need to add a path for MATLAB to get to /scripts? ###
matlab_command = "prepare_data('" + image + "','" + xml + "','" + data + "')"
os.system('matlab -r "' + matlab_command + '" -nosplash -wait')

# permute data
os.system('python ' + scripts + '/permute_data.py --filename=' + data)

# separate data
os.chdir(temp)
lines = open(title + '.data.P').readlines()
file_test = open(title + '.test','w')
file_train = open(title + '.train','w')
for i,l in enumerate(lines):
    if i < len(lines)/2:
        file_test.write(l)
    else:
        file_train.write(l)
file_test.close()
file_train.close()

# generate spec file
file_spec = open(title + '.spec','w')
file_spec.write("exampleTerminator=;\nattributeTerminator=,\nmaxBadExa=0\n")
file_spec.write("labels (0,1)\nH  number\nS  number\nV  number")
file_spec.close()
os.chdir(dir)

# run jboost to generate scoring function
os.chdir('../../jboost-2.1')
jboost_command = 'java jboost.controller.Controller '
jboost_input = '-S ' + temp + '/' + title + ' '
os.system(jboost_command + jboost_input + '-m ' + dir + '/scoring_function.m' )














