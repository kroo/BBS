function [ gradient_out ] = localgradient( x, y, img)
%LOCALGRADIENT Summary of this function goes here
%   Detailed explanation goes here
radius = 3

leftmost = max(x - radius, 1)
rightmost = min(x + radius, size(img, 2));
topmost = max(y - radius, 1)
bottommost = min(y + radius, size(img, 1));


for x = leftmost+1:rightmost-1
    for y = topmost-1:bottommost+1
        gradient_out(x - leftmost, y - topmost, 1) = (img(x-1, y) - image(x+1, y)) / 2.;
        gradient_out(x - leftmost, y - topmost, 2) = (img(x, y-1) - image(x, y+1)) / 2.;
    end
end

