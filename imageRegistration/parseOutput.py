import numpy as np

filename = "output.txt"
contents = open(filename)

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
currentRect = [np.array([0.,0.,1.]), np.array([0.,1.,1.]), np.array([1.,1.,1.]), np.array([1.,0.,1.])];
for obj in objects:
  obj['mat'] = np.array(obj['matrixElems']).reshape((3,3));
  # apply the transform
  currentRect = map(lambda x: np.inner(currentTransform, x), currentRect);
  
  # normalize (de-homogenize :))
  currentRect = map(lambda x: np.array([x[0]/x[2], x[1]/x[2], 1.0]), currentRect);
  currentTransform = np.inner(currentTransform, obj['mat']);
  
  print currentRect