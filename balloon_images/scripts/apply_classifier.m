function [] = apply_classifier(scorefunc,imagepath,outputfile)

	image = imread(imagepath);
	PicHSV=rgb2hsv(image);
	m = size(image,1);
	n = size(image,2);
	result = zeros(m,n);

	for i=1:m
		for j=1:n;
			pixel = squeeze(PicHSV(i,j,:));
			result(i,j) = scorefunc(pixel, [1,1,1]);
		end
	end

	imwrite(result,outputfile);