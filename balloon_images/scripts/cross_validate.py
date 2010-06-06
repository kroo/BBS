import sys,os

temp = 'C:\\Users\\Alex\\BBS\\balloon_images\\nfold_data_10'

# prepare data for jboost, then run it
for file in os.listdir(temp):
    os.chdir(temp)
    
    filepath = os.path.join(temp,file)
    stem_long = os.path.splitext(filepath)[0]
    stem_short = os.path.basename(stem_long)
    
    # delete unchanged data files
    if os.path.getsize(filepath) < 1:
        os.remove(filepath)
        continue
    
    # generate spec file
    spec = open(stem_long + '.spec','w')
    spec.write("exampleTerminator=;\nattributeTerminator=,\nmaxBadExa=0\n")
    spec.write("labels (0,1)\nH  number\nS  number\nV  number")
    spec.close()
    
    # prepare data for cross validation
    os.system('python ..\\scripts\\AddRandomIndex.py %s'%(stem_long))
    data = '%s_idx.data'%(stem_long)
    os.system('python ..\\scripts\\permuteData.py --filename=%s'%(data))
    os.rename(data,data + '.orig')
    os.rename(data + '.P',data)
    
    ###os.chdir('../../jboost-2.1')
    
    # perform cross validation
    nfold_options = '--booster=AdaBoost --folds=10 --rounds=10 --tree=ADD_ALL --generate'
    nfold_inputs = '--data=%s_idx.data --spec=%s_idx.spec'%(stem_long,stem_long)
    os.system('python ..\\scripts\\nfold.py %s %s'%(nfold_inputs,nfold_options))
    