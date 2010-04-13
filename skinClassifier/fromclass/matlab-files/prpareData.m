% Generate scatter plots of pixel values
Pic = imread('3people.jpeg');

h_PIC = imshow(Pic); 

poly=impoly(gca);

BW = createMask(poly,h_PIC);

figure;
imshow(BW);
%%
PicHSV=rgb2hsv(Pic);
PicH=PicHSV(:,:,1);
PicS=PicHSV(:,:,2);
PicV=PicHSV(:,:,3);

%%
% FaceH=BW .* (PicH+1);
% FaceS=BW .* (PicS+1);
% FaceV=BW .* (PicV+1);
% 
% NonH=(1-BW) .* (PicH+1);
% NonS=(1-BW) .* (PicS+1);
% NonV=(1-BW) .* (PicV+1);
% 
% imshow(NonH-1);
%%

fid = fopen('skinColor.data','w');
[m,n,k] = size(PicHSV);
for i=1:m
    for j=1:n;
        if(BW(i,j)==0)
            if(rand>0.03)
                continue;
            end
        end
        fprintf(fid, '%d,%3.2f,%3.2f,%3.2f;\n',BW(i,j),PicHSV(i,j,:));
    end
end
fclose(fid);

