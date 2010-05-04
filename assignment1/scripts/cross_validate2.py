import sys,os

script_path = '/Users/jeanne/school/cse151/BBS/jboost-2.1/scripts/' 
working_path = os.getcwd()
temp_path = os.path.join(working_path,'temp')
# stem is a movie name (10 digit number)
stem = os.path.basename(working_path)

# all new files created in temp directory
os.chdir(temp_path)

# generate spec file
spec = open(stem + '.spec','w')
spec.write("exampleTerminator=;\nattributeTerminator=,\nmaxBadExa=0\n")
spec.write("labels (0,1)\nH  number\nS  number\nV  number")
spec.close()

# prepare data for cross validation
os.system('python %s %s'%(os.path.join(script_path,'AddRandomIndex.py'),stem))
data = stem + '_idx.data'
os.system('python %s --filename=%s'%(os.path.join(script_path,'permuteData.py'),data))
os.rename(data,data + '.orig')
os.rename(data + '.P',data)

# perform cross validation
nfold_options = '--booster=AdaBoost --folds=10 --rounds=100 --tree=ADD_ROOT --generate'
nfold_inputs = '--data=%s_idx.data --spec=%s_idx.spec'%(stem,stem)
os.system('python %s %s %s'%(os.path.join(script_path,'nfold.py'),nfold_inputs,nfold_options))

# visualize results