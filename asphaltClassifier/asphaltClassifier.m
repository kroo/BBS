
%%
% Generate scatter plots of pixel values
ParkingLot = imread('data/parking_lot/image.png');
Mask = imread('data/parking_lot/mask.png');

h_ParkingLot = imshow(ParkingLot); 

%%
BW = im2bw(Mask);
inverseBW = 1-BW;

figure;

imshow(inverseBW);
%%
PicHSV=rgb2hsv(ParkingLot);
PicH=PicHSV(:,:,1);
PicS=PicHSV(:,:,2);
PicV=PicHSV(:,:,3);

imshow(ParkingLot); 
figure;
imshow(PicH);
figure;
imshow(PicS);
figure;
imshow(PicV);

%%

fid = fopen('asphalt.data','w');
fraction=.03;
negativetrainingset= if (rand < fraction && its false ) %put that thing into a matrix
    
[m,n,k] = size(PicHSV);
for i=1:m
    for j=1:n;
        if(inverseBW(i,j)==0)
            if(rand>0.03)
                continue;
            end
        end
        fprintf(fid, '%d,%3.2f,%3.2f,%3.2f;\n',inverseBW(i,j),PicHSV(i,j,:));
    end
end
fclose(fid);





