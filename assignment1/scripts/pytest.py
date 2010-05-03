#!/usr/bin/env python
import sys, os

print 'sys.argv[0] = ' + sys.argv[0]
print 'sys.path[0] = ' + sys.path[0]
print 'os.path.join(sys.path[0], sys.argv[0]) = ' + os.path.join(sys.path[0], sys.argv[0])

print 'os.getcwd() = ' + os.getcwd()

a = os.path.join(os.path.dirname(sys.path[0]),'jboost-2.1')
print 'complicated: ', a