function [ gradient_out ] = localgradient( x, y, img)
%LOCALGRADIENT Summary of this function goes here
%   Detailed explanation goes here
radius = 3;      %8 in the original paper, so we should put 9

leftmost = max(x - radius, 1);
rightmost = min(x + radius, size(img, 2));
topmost = max(y - radius, 1);
bottommost = min(y + radius, size(img, 1));

COLS = rightmost-leftmost-2;
ROWS = bottommost-topmost-2;
gradient_out = zeros(COLS,ROWS,2);

for x = leftmost+1:rightmost-1
    for y = topmost+1:bottommost-1
        dx = (img(y, x-1) - img(y, x+1)) / 2.;
        dy = (img(y-1, x) - img(y+1, x)) / 2.;
        % threshold "to enhance robustness to illumination change"
        t = .1*sqrt(255^2+255^2); 
        gradient_out(y - topmost, x - leftmost, 1) = min(sqrt(dx^2+dy^2),t);
        gradient_out(y - topmost, x - leftmost, 2) = atan(dy/dx);
    end
end

