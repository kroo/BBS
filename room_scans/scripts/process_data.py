#!/usr/bin/env python

import os,sys,shutil

testdir = '''C:\\Users\\Alex\\BBS\\room_scans\\img_22'''
temp = os.path.join(os.path.dirname(testdir),'img_22_processed')
os.mkdir(temp)
os.chdir(temp)

data = {'COUCH':open('COUCH.data','a'),
        'DOOR':open('DOOR.data','a'),
        'BROWN_WALL':open('BROWN_WALL.data','a'),
        'BLUE_WALL':open('BLUE_WALL.data','a'),
        'WHITE_WALL':open('WHITE_WALL.data','a')}

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
            # call MATLAB
            tempfilepath = os.path.join(temp,name + '_temp')
            matlab_command = "prepare_data('%s','%s','%s')"\
                             %(os.path.join(path,pic),
                               os.path.join(path,file),
                               tempfilepath)
            matlab_options = '-nodisplay -nodesktop -nosplash -wait'
            os.system('matlab %s -r "%s"'%(matlab_options,matlab_command))
            
            # counter
            counter += 1
            print '[%s/119]'%(counter)
            
            # append to data file
            tempfile = open(tempfilepath)
            shutil.copyfileobj(tempfile,data[name])
            tempfile.close()
            os.remove(tempfilepath)
        else:
            print '%s NOT in data near [%s/119]'%(name,counter)

    print

for item in data:
    data[item].close()