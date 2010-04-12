# Reads in a spec file and makes available all it's fields
# Evan Ettinger 4/20/09

import shutil
import os

################################################################
# specfile    - path to specfile
#
# Reads in spec file and return all necessary information
def ReadSpec(specfile):
    f = file(specfile,'r')
    labelArray = []
    exampleTerminator = None
    attributeTerminator = None
    for line in f:
        if(line.find('labels') >= 0):
            start = line.find('(')
            stop = line.find(')')
            labelArray = [x.strip() for x in line[start+1:stop].split(',')]
        if(line.find('exampleTerminator=') >= 0):
            indx = line.find('=')
            exampleTerminator = line[indx+1:].strip()
        if(line.find('attributeTerminator=') >= 0):
            indx = line.find('=')
            attributeTerminator = line[indx+1:].strip()
    f.close()
    return labelArray, exampleTerminator, attributeTerminator

