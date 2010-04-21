--[[
@title Minimalistic Intervalometer
@param a Shooting interval, min
@default a 0
@param b ...sec
@default b 10
--]]
 
Interval = a*60000 + b*1000
 
function TakePicture()
	press("shoot_half")
        repeat sleep(50) until get_shooting() == true
	press("shoot_full")
	release("shoot_full")
	repeat sleep(50) until get_shooting() == false	
        release "shoot_half"
end
 
repeat
	StartTick = get_tick_count()
	TakePicture()
	sleep(Interval - (get_tick_count() - StartTick))
until false