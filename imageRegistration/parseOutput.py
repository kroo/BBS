import numpy as np
from struct import *
import cairo

iwidth = 1280 * 2.
iheight = 720 * 2.

filename = "output3.txt"
contents = open(filename)
surface = cairo.SVGSurface(filename + '.svg', iwidth, iheight)
cr = cairo.Context(surface)
outfilename = "images/imageindex.bin"
bout = open(outfilename, 'w');

cr.set_line_width(2.0)
cr.set_source_rgb(0, 0, 0)


state = "NEW";
objects = [];
index = 0;
for line in contents.readlines():
  if state == "NEW":
    objects.append({'name': line.split(":")[0], 'matrixElems': [] })
    state = "PARSING"
  elif state == "PARSING":
    objects[index]['matrixElems'] += map(float, filter(lambda x: len(x.strip()) > 0, line.split(" ")))
    if len(objects[index]['matrixElems']) == 9:
      index += 1;
      state = "NEW"


currentTransform = np.identity(3);
currentRect = [np.array([0.,0.,1.]), np.array([0.,720.,1.]), np.array([1280.,720.,1.]), np.array([1280.,0.,1.])];
for obj in objects:
  obj['mat'] = (np.array(obj['matrixElems']).reshape((3,3)));
  currentTransform = obj['mat']
  # apply the transform
  currentRect = map(lambda x: np.inner(x, obj['mat']), currentRect);
  # normalize (de-homogenize :))
  currentRect = map(lambda x: np.array([x[0]/x[2], x[1]/x[2], 1.0]), currentRect);

  output = pack("32s9f", obj['name'], currentTransform[0,0],currentTransform[0,1],currentTransform[0,2],
                                      currentTransform[1,0],currentTransform[1,1],currentTransform[1,2],
                                      currentTransform[2,0],currentTransform[2,1],currentTransform[2,2]);


  print currentTransform

  cr.move_to(currentRect[0][0], currentRect[0][1])
  cr.line_to(currentRect[1][0], currentRect[1][1])
  cr.line_to(currentRect[2][0], currentRect[2][1])
  cr.line_to(currentRect[3][0], currentRect[3][1])
  cr.close_path()
  cr.stroke()
  
  bout.write(output);
  
  currentRect = [np.array([0.,0.,1.]), np.array([0.,720.,1.]), np.array([1280.,720.,1.]), np.array([1280.,0.,1.])];

bout.flush();
bout.close();