function [] = apply_classifier(imagepath,outputfile)

	image = imread(imagepath);
	PicHSV=rgb2hsv(image);
	m = 480;
	n = 640;
	result = zeros(m,n);

	for i=1:m
		for j=1:n;
			pixel = squeeze(PicHSV(i,j,:));
			result(i,j) = predict(pixel, [1,1,1]);
		end
	end

	imwrite(result,outputfile);