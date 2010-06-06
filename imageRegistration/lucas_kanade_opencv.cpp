// from http://dasl.mem.drexel.edu/~noahKuntz/openCVTut9.html#Step%201

#include <cv.h>
#include <highgui.h>


void PrintMat(CvMat *A)
{
  int i, j;
  for (i = 0; i < A->rows; i++)
  {
    printf("\n"); 
    switch (CV_MAT_DEPTH(A->type))
    {
      case CV_32F:
      case CV_64F:
      for (j = 0; j < A->cols; j++)
        printf ("%8.3f ", (float)cvGetReal2D(A, i, j));
      break;
      case CV_8U:
      case CV_16U:
      for(j = 0; j < A->cols; j++)
        printf ("%6d",(int)cvGetReal2D(A, i, j));
      break;
      default:
      break;
    }
  }
  printf("\n");
}

void calcNecessaryImageRotation(IplImage *src) {
#define MAX_LINES 100
  CvMemStorage* storage = cvCreateMemStorage(0);
  CvSize img_sz = cvGetSize( src );

	IplImage* color_dst = cvCreateImage( img_sz, 8, 3 );
	IplImage* dst = cvCreateImage( img_sz, 8, 1 );
  CvSeq* lines = 0;
  int i;
  
  cvCanny( src, dst, 50, 200, 3 );
  cvCvtColor( dst, color_dst, CV_GRAY2BGR );
	
  cvSaveImage("canny.png", dst);
  
  lines = cvHoughLines2( dst,
                         storage,
                         CV_HOUGH_PROBABILISTIC,
                         1,
                         CV_PI/180,
                         80,
                         30,
                         10 );
  for( i = 0; i < lines->total; i++ )
  {
      CvPoint* line = (CvPoint*)cvGetSeqElem(lines,i);
      cvLine( color_dst, line[0], line[1], CV_RGB(255,0,0), 1, 8 );
      printf("%f\n", atan((double)(line[1].y-line[0].y) / (double)(line[1].x-line[0].x)));
  }

  // TODO(kroo): build up a smoothed histogram (cvHistogram)
  // TODO(kroo): find two peaks, assert that they are separated by roughly 90˚
  // TODO(kroo): find smallest rotation necessary to cause the lines to point straight up/down/left/right
  

  cvSaveImage("hough.png", color_dst);

  cvNamedWindow( "Hough Transform", 1 );
  cvShowImage( "Hough Transform", color_dst );
  
  cvWaitKey(0);

}

const int MAX_CORNERS = 500;
void processImagePair(const char *file1, const char *file2, CvVideoWriter *out, struct CvMat *currentOrientation) {
  // Load two images and allocate other structures
	IplImage* imgA = cvLoadImage(file1, CV_LOAD_IMAGE_GRAYSCALE);
	IplImage* imgB = cvLoadImage(file2, CV_LOAD_IMAGE_GRAYSCALE);
	IplImage* imgBcolor = cvLoadImage(file2);
 
	CvSize img_sz = cvGetSize( imgA );
	int win_size = 15;
  
	// Get the features for tracking
	IplImage* eig_image = cvCreateImage( img_sz, IPL_DEPTH_32F, 1 );
	IplImage* tmp_image = cvCreateImage( img_sz, IPL_DEPTH_32F, 1 );
 
	int corner_count = MAX_CORNERS;
	CvPoint2D32f* cornersA = new CvPoint2D32f[ MAX_CORNERS ];
 
	cvGoodFeaturesToTrack( imgA, eig_image, tmp_image, cornersA, &corner_count,
		0.05, 3.0, 0, 3, 0, 0.04 );
 
  fprintf(stderr, "%s: Corner count = %d\n", file1, corner_count);
 
	cvFindCornerSubPix( imgA, cornersA, corner_count, cvSize( win_size, win_size ),
		cvSize( -1, -1 ), cvTermCriteria( CV_TERMCRIT_ITER | CV_TERMCRIT_EPS, 50, 0.03 ) );
 
	// Call Lucas Kanade algorithm
	char features_found[ MAX_CORNERS ];
	float feature_errors[ MAX_CORNERS ];
 
	CvSize pyr_sz = cvSize( imgA->width+8, imgB->height/3 );
 
	IplImage* pyrA = cvCreateImage( pyr_sz, IPL_DEPTH_32F, 1 );
	IplImage* pyrB = cvCreateImage( pyr_sz, IPL_DEPTH_32F, 1 );
 
	CvPoint2D32f* cornersB = new CvPoint2D32f[ MAX_CORNERS ];
 
  calcNecessaryImageRotation(imgA);
 
	cvCalcOpticalFlowPyrLK( imgA, imgB, pyrA, pyrB, cornersA, cornersB, corner_count, 
		cvSize( win_size, win_size ), 5, features_found, feature_errors,
		 cvTermCriteria( CV_TERMCRIT_ITER | CV_TERMCRIT_EPS, 20, 0.3 ), 0 );
 
   CvMat *transform = cvCreateMat(3,3, CV_32FC1);
   CvMat *invTransform = cvCreateMat(3,3, CV_32FC1);
	// Find a homography based on the gradient
   CvMat cornersAMat = cvMat(1, corner_count, CV_32FC2, cornersA);
   CvMat cornersBMat = cvMat(1, corner_count, CV_32FC2, cornersB);
   cvFindHomography(&cornersAMat, &cornersBMat, transform, CV_RANSAC, 15, NULL);

   cvInvert(transform, invTransform);
   cvMatMul(currentOrientation, invTransform, currentOrientation);
   // save the translated image
 	 IplImage* trans_image = cvCloneImage(imgBcolor);
   cvWarpPerspective(imgBcolor, trans_image, currentOrientation, CV_INTER_CUBIC+CV_WARP_FILL_OUTLIERS);

   printf("%s:\n", file1);
   PrintMat(currentOrientation);

  // cvSaveImage(out, trans_image);
  cvWriteFrame(out, trans_image);

  cvReleaseImage(&eig_image);
  cvReleaseImage(&tmp_image);  
  cvReleaseImage(&trans_image);
  cvReleaseImage(&imgA);
  cvReleaseImage(&imgB);
  cvReleaseImage(&imgBcolor);
  cvReleaseImage(&pyrA);
  cvReleaseImage(&pyrB);
  
  cvReleaseData(transform);
  delete [] cornersA;
  delete [] cornersB;
  
  
}


#if 1

int main(int argc, char *argv[]) {
  IplImage* src;
  if( argc == 2 && (src=cvLoadImage(argv[1], 0))!= 0)
  {
    calcNecessaryImageRotation(src);
  }
}

#else
int main(int argc, char* argv[])
{

  int frame = 11;
  CvMat *orientation = cvCreateMat(3,3,CV_32FC1);
  cvSetIdentity(orientation);
  CvVideoWriter *writer = 0;
  int isColor = 1;
  int fps     = 30;
  int frameW  = 1280;
  int frameH  = 720;
  writer=cvCreateVideoWriter("/Users/kroo/Desktop/out.avi",-1,
    fps,cvSize(frameW,frameH),isColor);
 
 
  int firstFrame = 22560;
  int lastFrame = 26640;
  
  for(frame = firstFrame+1; frame<lastFrame-1; frame++) {
    char firstImage  [32];
    char secondImage [32];
    // char outImage    [64];
    sprintf(firstImage, "frame%d.ppm", frame);
    sprintf(secondImage, "frame%d.ppm", frame+1);
    // sprintf(outImage, "global_frame%d.png", frame+1);
    processImagePair(firstImage, secondImage, writer, orientation);
  }
 
  cvReleaseVideoWriter(&writer);
 
	return 0;
}
#endif