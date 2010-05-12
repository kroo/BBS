#! /usr/bin/env python

import os
import sys
import random
import getopt
import re
import struct

NUM_FILES = 256
MAX_BYTES = 1024*1024*256

def usage():
    print 'Usage: permuteData.py <--filename=NAME> [--binary --blocksize=N]'
    print '\t--filename=NAME   Specifies a path to a file to permute the data elements.  Output will be placed in file named <NAME>.P'
    print '\t--binary          Indicates that filename is a binary file.  --blocksize is required then.  If not indicated, the file is assumed to be a text file whose rows need to be permuted.'
    print '\t--blocksize=N     Required parameter with the --binary flag, and is a positive integer indicating how large each data element is in bytes.'

def AsciiPermute(inName):
    fileSize = int(os.path.getsize(inName))
    if(fileSize < MAX_BYTES):
        f = file(inName,'r')
        data = f.readlines()
        f.close()
        random.shuffle(data)

        f = file('%s.P' % (inName),'w')
        f.writelines(data)
        f.close()
    else:
        #Create temp dir
        dirName = './' + inName + '.pdir'
        os.mkdir(dirName,0744)
        #How many temp files do we need?
        numTempFiles = int(2*fileSize / MAX_BYTES)
        if(numTempFiles > NUM_FILES):
            numTempFiles = NUM_FILES

        #Create temp files
        tempFile = [[None] for i in range(numTempFiles)]
        tempFileName = [[None] for i in range(numTempFiles)]
        for i in range(numTempFiles):
            tempFileName[i] = dirName + '/tmp%d' % (i)
            tempFile[i] = file(tempFileName[i],'w')
            
        print 'Reading ' + inName + '...'
        f = file(inName,'r')        
        for line in f:
            tempFile[random.randint(0,numTempFiles - 1)].write(line)
        f.close()

        #Recurse and collect
        for i in range(numTempFiles):
            tempFile[i].close()
            print 'Doing iteration %d of %d' % (i,numTempFiles-1)
            command = './permuteData.py --filename=%s > /dev/null' % (tempFileName[i])
            os.system(command)
            command = 'cat %s.P >> %s.P' % (tempFileName[i],inName)
            os.system(command)
            command = 'rm %s %s.P' % (tempFileName[i], tempFileName[i])
            os.system(command)

        #Cleanup
        command = 'rm -rf %s' %(dirName)
        print 'Cleaning up...'
        os.system(command)

def BinaryPermute(inName, blockSize):
    fileSize = int(os.path.getsize(inName))
    if(fileSize < MAX_BYTES):
        f = file(inName,'rb')
        data = []
        while(1):
            nextBlock = f.read(blockSize)
            if(nextBlock == ''):
                break
            data.append(nextBlock)
        random.shuffle(data)

        f = file('%s.P' % (inName),'wb')
        for i in range(len(data)):
            f.write(data[i])
        f.close()
    else:
        #Make temp dir
        dirName = './' + inName + '.pdir'
        os.mkdir(dirName,0744)
        #How many temp files do we need?
        numTempFiles = int(2*fileSize / MAX_BYTES)
        if(numTempFiles > NUM_FILES):
            numTempFiles = NUM_FILES
            
        #Create temp files
        tempFile = [[None] for i in range(numTempFiles)]
        tempFileName = [[None] for i in range(numTempFiles)]
        for i in range(numTempFiles):
            tempFileName[i] = dirName + '/tmp%d' % (i)
            tempFile[i] = file(tempFileName[i],'w')
        print 'Reading ' + inName + '...'
        f = file(inName,'rb')
        while(1):
            nextBlock = f.read(blockSize)
            if(nextBlock == ''):
                break
            tempFile[random.randint(0,numTempFiles - 1)].write(nextBlock)
        f.close()

        #Recurse and collect
        for i in range(numTempFiles):
            tempFile[i].close()
            print 'Doing iteration %d of %d' % (i,numTempFiles-1)
            command = './permuteData.py --filename=%s --binary --blocksize=%d > /dev/null' % (tempFileName[i],blockSize)
            os.system(command)
            command = 'cat %s.P >> %s.P' % (tempFileName[i],inName)
            os.system(command)
            command = 'rm %s %s.P' % (tempFileName[i], tempFileName[i])
            os.system(command)

        #Cleanup
        command = 'rm -rf %s' %(dirName)
        print 'Cleaning up...'
        os.system(command)
        
def main():
    try:
        opts, args = getopt.getopt(sys.argv[1:],'',
                                   ['filename=','binary','blocksize='])
    except getopt.GetoptError:
        print 'permuteData.py: Illegal argument\n'
        usage()
        sys.exit(2)
        
    filename = None
    isBinary = 0
    blockSize = -1

    for opt,arg in opts:
        if(opt == '--filename'):
            filename = arg
        elif(opt == '--binary'):
            isBinary = 1
        elif(opt == '--blocksize'):
            blockSize = int(arg)
        else:
            print 'The argument ' + arg + ' is not a recognized option.\n'
            usage()
            sys.exit(2)

    if(filename == None):
        print '--filename is a required parameter!\n'
        usage()
        sys.exit(2)

    if(isBinary == 1 and blockSize <= 0):
        print '--blocksize is a required parameter with --binary and must be a positive integer!\n'
        usage()
        sys.exit(2)

    if(not os.path.isfile(filename)):
        print '%s does not exist!\n' % (filename)
        usage()
        sys.exit(2)
        
    random.seed()
    if(isBinary):
        BinaryPermute(filename, blockSize)
    else:
        AsciiPermute(filename)


if __name__ == "__main__":
    main()
            
