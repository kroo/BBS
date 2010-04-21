image = imread('../data/launch1/4535627883_855d92a565_b.jpg');

imageHSV = rgb2hsv(image);
imageV = imageHSV(:,:,3);


layers = 2 : 8;

imageA = zeros(size(layers, 2), size(imageV, 1), size(imageV, 2));
imageB = zeros(size(layers, 2), size(imageV, 1), size(imageV, 2));
diff = zeros(size(layers, 2), size(imageV, 1), size(imageV, 2));

imageA(1,:,:) = blur(imageV, sqrt(2));
imageB(1,:,:) = blur(squeeze(imageA(1,:,:)), sqrt(2));

diffImg = squeeze(imageA(1,:,:) - imageB(1,:,:));
diff(1,:,:) = diffImg;
%imtool((diffImg - diffmin) / (diffmax - diffmin));
disp('Layer 1 complete, cap`n!');

imgsize = size(squeeze(imageA(1,:,:)));
for layer = layers;
   disp('starting next layer');
   
   % save the old imageB as the "last image"
   lastImage = squeeze(imageB(layer - 1,:,:));
   
   % calculate new image size, save the old size.
   oldsize = imgsize;
   newsize = floor(imgsize / 1.5);
   imgsize = newsize;
   
   % calculate the resized oldImageB for the newImageA
   newImageA = imgrescale(lastImage(1:oldsize(1), 1:oldsize(2)), newsize);
   imageA(layer,:,:) = padarray(newImageA(:,:), (size(imageV) - newsize), 0.0, 'post');
   disp('Image A complete, cap`n!');
   
   % calculate the newImageB as a blurred imageA
   newImageB = blur(newImageA, sqrt(2));;
   imageB(layer,:,:) = squeeze(padarray(newImageB, (size(imageV) - newsize), 0.0, 'post'));
   disp('Image B complete, cap`n!');
   
   % calculate DoGs
   diffImg = squeeze(newImageA - newImageB);
   diff(layer,:,:) = padarray(diffImg, (size(imageV) - newsize), 0.0, 'post');
   diffmin = min(diffImg(:));
   diffmax = max(diffImg(:));

   imtool((diffImg - diffmin) / (diffmax - diffmin));
   disp('Layer complete!');
end

%% now we have an image pyramid.  time to build a featureset

for layer = 1:(size(layers,2)+1);
    DoG = squeeze(diff(layer,:,:));
    [vmax, imax, vmin, imin] = extrema2(DoG);
    [ymax, xmax]  = ind2sub(size(DoG), imax);
    [ymin, xmin]  = ind2sub(size(DoG), imin);
    nfeatures = 0;
    clear features

    for feature = [xmin,ymin,vmin; xmax,ymax,vmax]';
        x = feature(1);
        y = feature(2);
        val = feature(3);
        % check up, down, left, and right.
        threshold = 0.005;
        left  = ((x == 1)                                   || abs(diff(layer, y, x-1) - val) > threshold);
        right = ((x == size(imageV, 2) * 1/1.5 ^ (layer-1)) || abs(diff(layer, y, x+1) - val) > threshold);
        up    = ((y == 1)                                   || abs(diff(layer, y-1, x) - val) > threshold);
        down  = ((y == size(imageV, 1) * 1/1.5 ^ (layer-1)) || abs(diff(layer, y+1, x) - val) > threshold);
        above = ((layer == size(layers, 2) + 1)             || abs(diff(layer+1, 1+floor((y-1)/1.5), 1+floor((x-1)/1.5)) - val) > threshold);
        below = ((layer == 1)                               || abs(diff(layer-1, 1+floor((y-1)*1.5), 1+floor((x-1)*1.5)) - val) > threshold);
        if(left && right && up && down && above && below)
           nfeatures = nfeatures + 1;
           features(nfeatures, :) = [x, y, val];
        end
        
    end
    
    figure
    imshow((DoG - min(DoG(:))) / (max(DoG(:) - min(DoG(:)))))
    hold on
    plot(features(:,1,:), features(:, 2, :), 'r+');
    hold off
end
