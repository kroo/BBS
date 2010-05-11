#!/opt/local/bin/python2.6
import Image,os,sys

step = 2
if step is 1: # build difference image
  im = Image.open("frame4801.ppm")
  master = list(im.copy().getdata())
  numimages = 1

  sequence = map(lambda arg:'frame%d.ppm'%(arg),range(4801,6600))
  for filename in os.listdir(os.getcwd()):
    if filename in sequence:
      print filename
      add = Image.open(filename).getdata()
      master = [(x[0]+master[i][0],x[1]+master[i][1],x[2]+master[i][2]) for i,x in enumerate(add)]
      numimages += 1

  master = map(lambda x:(x[0]/numimages,x[1]/numimages,x[2]/numimages),master)
  im.putdata(master)
  im.save("difference.png")

if step is 2: # calculate average for each of the components, then subtract
  import numpy as np
  import matplotlib.mlab as mlab
  import matplotlib.pyplot as plt

  im = Image.open("difference.png")
  print "loading data"
  x = im.getdata()
  print "calculating average"
  avg = 3*[0]
  fmax = 3*[0]
  fmin = 3*[255]
  num = 0;
  for pix in x:
    num += 1
    for indx in range(3):
      avg[indx] += pix[indx]
      fmax[indx] = max(fmax[indx], pix[indx])
      fmin[indx] = min(fmin[indx], pix[indx])
  avg = map(lambda x: x / num, avg)

  print "Average color:", avg
  print "Max:", fmax
  print "Min:", fmin
  # n, bins, patches = plt.hist(x, 50, histtype='barstacked')
  # plt.grid(True)
  # plt.show()
  # print n, bins, patches
  exframe = Image.open("frame22822.ppm")
  dat = list(exframe.getdata())
  n, bins, patches = plt.hist(dat, 255, histtype='barstacked')
  plt.grid(True)
  plt.show()
  for i, pix in enumerate(dat):
    newpix = list(dat[i])
    for indx in range(3):
      newpix[indx] = pix[indx] - (x[i][indx] - avg[indx])
    dat[i] = tuple(newpix)
  im.putdata(dat)
  im.save("frame22822_fixed.ppm")
  