files = dir('*.jpg');

m = 480; n = 640;
master = zeros(m,n,3,'uint16');

for i = 1:numel(files)
    master = master + uint16(imread(files(i).name));
end
master = master ./ numel(files);
imwrite(uint8(master),'naive_average.jpg');