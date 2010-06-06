files = dir('*.jpg');

m = 720; n = 1280;
master = zeros(m,n,'uint16');

for i = 1:numel(files)
    master = master + uint16(imread(files(i).name));
end
master = master ./ numel(files);
imwrite(uint8(master),'mask_average.jpg');