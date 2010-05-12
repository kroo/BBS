#!/usr/bin/env python

import os,shutil

datadir = 'C:\\Users\\Alex\\BBS\\room_scans\\original_data'
imagedir = 'C:\\Users\\Alex\\BBS\\room_scans\\images_only'

for degree in ['-45','-22','0','22']:
    
    stem = 'img_' + degree
    moveto = os.path.join(imagedir,stem)
    os.mkdir(moveto)
    
    # get data from jpg and xml files
    for path,dirs,files in os.walk(os.path.join(datadir,stem)):
        
        # skip the first iteration
        if (len(files) == 0):
            continue
        
        pic = os.path.basename(path) + '.jpg'
        source = os.path.join(path,pic)
        dest = os.path.join(moveto,pic)
        shutil.copy(source,dest)
        print pic, 'moved from',source,'to',dest