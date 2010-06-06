function [] = prepare_data(imagepath,xmlpath,outputfile)

    image = imread(imagepath);
    xml = xmlread(xmlpath);

    m = size(image,1);
    n = size(image,2);
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

    BW(BW>1) = 1; % some polygons may overlap

    imageHSV=rgb2hsv(image);

    fid = fopen(outputfile,'w');

    nNegativeTrainingExamples = size(BW(BW==0),1);
    nPositiveTrainingExamples = size(BW(BW==1),1);

    for i=1:m
        for j=1:n;
            %if(BW(i,j)==0)
            %    if(rand>nPositiveTrainingExamples/nNegativeTrainingExamples)
            %        continue;
            %    end
            %end
            fprintf(fid, '%d,%3.2f,%3.2f,%3.2f;\n',BW(i,j),imageHSV(i,j,:));
        end
    end
    fclose(fid);
    exit;


