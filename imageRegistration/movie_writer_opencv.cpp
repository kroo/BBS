#include <cv.h>
#include <highgui.h>
#include <cvaux.h>
#include <cxcore.h>

int main()
{
  CvVideoWriter *writer = 0;
  int isColor = 1;
  int fps     = 5;  // or 30
  int frameW  = 1280; //640; // 744 for firewire cameras
  int frameH  = 720; //480; // 480 for firewire cameras
//writer=cvCreateVideoWriter("out.avi",CV_FOURCC('P','I','M','1'),
//                           fps,cvSize(frameW,frameH),isColor);
  writer=cvCreateVideoWriter("/Users/kroo/Desktop/out.avi",-1,
    fps,cvSize(frameW,frameH),isColor);
  IplImage* img = 0; 

  img=cvLoadImage("global_frame752.png");
  cvWriteFrame(writer,img);      // add the frame to the file
  img=cvLoadImage("global_frame753.png");
  cvWriteFrame(writer,img);
  img=cvLoadImage("global_frame754.png");
  cvWriteFrame(writer,img);
  img=cvLoadImage("global_frame755.png");
  cvWriteFrame(writer,img);
  img=cvLoadImage("global_frame756.png");
  cvWriteFrame(writer,img);
  img=cvLoadImage("global_frame757.png");
  cvWriteFrame(writer,img);

  cvReleaseVideoWriter(&writer);
  return 0;
}
