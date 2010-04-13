%%
% Generate scatter plots of pixel values
ParkingLot = imread('../data/parking_lot/test-lot.png');
Mask = imread('../data/parking_lot/mask.png');

%%
BW = im2bw(Mask);
inverseBW = 1-BW;

%%
PicHSV=rgb2hsv(ParkingLot);
PicH=PicHSV(:,:,1);
PicS=PicHSV(:,:,2);
PicV=PicHSV(:,:,3);

%%
[m,n,k] = size(PicHSV);
result = zeros([m,n]);
defined = [1,1,1];
pixel = [0,0,0];


for i=1:m
    if mod(i, 10) == 0
        disp(i);
    end
    for j=1:n;
        pixel = squeeze(PicHSV(i,j,:));
        result(i,j) = asphaltClassifier(pixel, defined);
    end
end

imshow(result);