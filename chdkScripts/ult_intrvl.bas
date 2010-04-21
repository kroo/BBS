rem Author - Keoeeit                     
rem Written for S-Series                     
rem Should be okay on others                     
rem Use Endless mode with caution                     
rem See documentation for important info                     
rem first version                     
@title Ultra Intervalometer                     
@param a Delay 1st Shot (Mins)                     
@default a 0                     
@param b Delay 1st Shot (Secs)                     
@default b 0                     
@param c Number of Shots                     
@default c 5                     
@param d Interval (Minutes)                     
@default d 0                     
@param e Interval (Seconds)                     
@default e 0                     
@param f Interval (10th Seconds)                     
@default f 5                     
@param g Endless? No=0 Yes=1                     
@default g 0                     
p=a*60000+b*1000                     
t=d*60000+e*1000+f*100                     
if c<1 then let c=5                     
if t<100 then let t=100                     
if g<0 then let g=0                     
if g>1 then let g=1                     
if p<0 then let p=0                     
z=t*c                     
y=p+z                     
print "1 Cycle Time:", y/60000; "min", y%60000/1000; "sec"                     
goto "interval"                     
:interval                     
  if p>0 then gosub "pause"                     
  print "Shot 1 of", c                     
  shoot                     
  if c=1 then end                     
  for n=2 to c                     
  sleep t                     
  print "Shot", n, "of", c                     
  shoot                     
  next n                     
  if g=1 then goto "interval" else end                     
:pause                     
  n=(a*60)+b                     
  for m=1 to n                     
  q=n-m                     
  print "Intvl Begins:", q/60; "min", q%60; "sec"                     
  sleep 930                     
  next m                     
  return
