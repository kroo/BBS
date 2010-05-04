#! /usr/bin/env python

import sys, os

# check for inputs
if len(sys.argv) == 1:
    print 'Usage: Pass in an image and an xml file'
    print 'Run from same directory as passed files'
    sys.exit()

# store inputs
image = sys.argv[1]
xml = sys.argv[2]

# store paths, make temp directory
###script_path = os.path.join(os.path.dirname(sys.path[0]),'jboost-2.1','scripts')
###script_path = 'C:\Users\Alex\BBS\jboost-2.1\scripts'
script_path = '/Users/jeanne/school/cse151/BBS/jboost-2.1/scripts/' 
working_path = os.getcwd()
temp_path = os.path.join(working_path,'temp')
os.mkdir(temp_path)

# stem is a movie name (10 digit number)
stem = os.path.basename(working_path)

# call MATLAB to get data
data = os.path.join(temp_path,stem + '.data')
matlab_command = "prepare_data('%s','%s','%s')"%(image,xml,data)
matlab_options = '-nodisplay -nodesktop -nosplash -wait'
os.system('matlab -maci %s -r "%s" &'%(matlab_options,matlab_command))