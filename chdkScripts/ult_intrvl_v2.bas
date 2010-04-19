rem Author - Keoeeit                 
rem Upgraded by Mika Tanninen                 
@title Ultra Intervalometer                 
@param a Delay 1st Shot (Mins)                 
@default a 0                 
@param b Delay 1st Shot (Secs)                 
@default b 0                 
@param c Number of Shots (0 inf)                 
@default c 0                 
@param d Interval (Minutes)                 
@default d 0                 
@param e Interval (Seconds)                 
@default e 10                 
@param f Interval (10th Seconds)                 
@default f 0                 
n=0
t=(d*600+e*10+f)*100                 
if c<1 then let c=0                 
if t<100 then let t=100                 
g=(a*60)+b
if g<=0 then goto "interval"                 
for m=1 to g                 
 print "Intvl Begins:", (g-m)/60; "min", (g-m)%60; "sec"                 
 sleep 930                 
 next m                 
:interval               
  n=n+1
  if c=0 then print "Shot", n else print "Shot", n, "of", c                 
  shoot                 
  if n=c then end                 
  sleep t                 
  goto "interval"  