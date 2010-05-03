#! /usr/bin/env python

import os,sys

script_path = os.path.join(os.path.dirname(sys.path[0]),'jboost-2.1','scripts')
###script_path = 'C:\Users\Alex\BBS\jboost-2.1\scripts'
###script_path = '/Users/jeanne/school/cse151/BBS/jboost-2.1/scripts/'
working_path = os.getcwd()
# stem is a movie name (10 digit number)
stem = os.path.basename(working_path)
temp_path = os.path.join(working_path,'temp2')
data = os.path.join(temp_path,stem + '.data')

# all new files created in temp directory
os.chdir(temp_path)

# permute data
os.system('python %s --filename=%s'%(os.path.join(script_path,'permuteData.py'),data))

# separate data
lines = open(stem + '.data.P').readlines()
test = open(stem + '.test','w')
train = open(stem + '.train','w')
for i,l in enumerate(lines):
    if i < len(lines)/2:
        test.write(l)
    else:
        train.write(l)
test.close()
train.close()

# generate spec file
spec = open(stem + '.spec','w')
spec.write("exampleTerminator=;\nattributeTerminator=,\nmaxBadExa=0\n")
spec.write("labels (0,1)\nH  number\nS  number\nV  number")
spec.close()

# run jboost to generate scoring function
jboost_command = 'jboost.controller.Controller -cp %s'%(os.path.dirname(script_path))
jboost_input = '-S %s'%(os.path.join(temp_path,stem))
jboost_output = '-m %s'%(temp_path + '/predict.m')
os.system('java %s %s %s'%(jboost_command,jboost_input,jboost_output))