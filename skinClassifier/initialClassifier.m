pic = imread('1263253242_0000002.jpg');
xml = xmlread('0000002.xml');

m = 480; n = 640;
BW = zeros(m,n);
polygonList = xml.getElementsByTagName('polygon');
for i = 0:polygonList.getLength()-1;
    % within a polygon %
    pointsList = polygonList.item(i);
    npts = pointsList.getLength()-1; % first entry is 'username'
    x = zeros(1,npts);
    y = zeros(1,npts);
    
    for j = 1:npts;
        p = pointsList.item(j);
        x(j) = str2num(p.getFirstChild.getFirstChild.getNodeValue);
        y(j) = str2num(p.getLastChild.getFirstChild.getNodeValue);
    end
    BW = BW + poly2mask(x, y, m, n);    
end

