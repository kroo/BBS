% Demonstration script file for image rescaling with IMGRESCALE.

% Author : Andreas Klimke, Universität Stuttgart
% Version: 1.0
% Date   : June 17, 2003

X = imread('test.jpg');

subplot(3,1,1);
imagesc(X);
axis equal; axis tight;
title('Original image');

subplot(3,1,2);
imagesc(imgrescale(X, [size(X,1) size(X,2)] .*2/3));
axis equal; axis tight;
title('Rescaled image, bilinear interpolation, 66%');

subplot(3,1,3);
imagesc(imgrescale(X, [size(X,1) size(X,2)] .*3/2));
axis equal; axis tight;
title('Rescaled image, bilinear interpolation, 150%');
