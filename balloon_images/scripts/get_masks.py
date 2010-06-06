#!/usr/bin/env python

import os,sys,shutil

testdir = 'C:\\Users\\Alex\\BBS\\balloon_images\\original_data'
temp = os.path.join(os.path.dirname(testdir),'masks')
os.mkdir(temp)
os.chdir(temp)

data = ['CAR']

counter = 0

# get data from jpg and xml files
for path,dirs,files in os.walk(testdir):
    
    # skip the first iteration
    if (len(files) == 0):
        continue
    
    stem = os.path.basename(path)
    pic = stem + '.jpg'
    xml = stem + '.xml'
    
    files.remove(pic)
    files.remove(xml)
    
    for file in files:
        name = os.path.splitext(file)[0]
        if name in data:
            # counter
            counter += 1
            print '[%s/4]'%(counter)
            
            # call MATLAB
            outputfilepath = os.path.join(temp,'mask_' + pic)
            matlab_command = "extract_mask('%s','%s','%s')"\
                             %(os.path.join(path,pic),
                               os.path.join(path,file),
                               outputfilepath)
            matlab_options = '-nodisplay -nodesktop -nosplash -wait'
            os.system('matlab %s -r "%s"'%(matlab_options,matlab_command))
            
        else:
            print '%s NOT in data near [%s/4]'%(name,counter)