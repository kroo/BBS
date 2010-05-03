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

const int MAX_CORNERS = 500;
void processImagePair(const char *file1, const char *file2, const char *out) {
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
		0.05, 5.0, 0, 3, 0, 0.04 );
 
	cvFindCornerSubPix( imgA, cornersA, corner_count, cvSize( win_size, win_size ),
		cvSize( -1, -1 ), cvTermCriteria( CV_TERMCRIT_ITER | CV_TERMCRIT_EPS, 20, 0.03 ) );
 
	// Call Lucas Kanade algorithm
	char features_found[ MAX_CORNERS ];
	float feature_errors[ MAX_CORNERS ];
 
	CvSize pyr_sz = cvSize( imgA->width+8, imgB->height/3 );
 
	IplImage* pyrA = cvCreateImage( pyr_sz, IPL_DEPTH_32F, 1 );
	IplImage* pyrB = cvCreateImage( pyr_sz, IPL_DEPTH_32F, 1 );
 
	CvPoint2D32f* cornersB = new CvPoint2D32f[ MAX_CORNERS ];
 
	cvCalcOpticalFlowPyrLK( imgA, imgB, pyrA, pyrB, cornersA, cornersB, corner_count, 
		cvSize( win_size, win_size ), 5, features_found, feature_errors,
		 cvTermCriteria( CV_TERMCRIT_ITER | CV_TERMCRIT_EPS, 20, 0.3 ), 0 );
 
   CvMat *transform = cvCreateMat(3,3, CV_32FC1);
	// Find a homography based on the gradient
   CvMat cornersAMat = cvMat(1, corner_count, CV_32FC2, cornersA);
   CvMat cornersBMat = cvMat(1, corner_count, CV_32FC2, cornersB);
   cvFindHomography(&cornersAMat, &cornersBMat, transform, CV_LMEDS, 0, NULL);

   // save the translated image
 	 IplImage* trans_image = cvCloneImage(imgBcolor);
   cvWarpPerspective(imgBcolor, trans_image, transform, CV_WARP_INVERSE_MAP+CV_WARP_FILL_OUTLIERS);

   printf("%s:\n", out);
   PrintMat(transform);

  cvSaveImage(out, trans_image);
}

 
int main(int argc, char* argv[])
{

  int frame = 11;
  
  for(frame = 11; frame<100; frame++) {
    char firstImage  [32];
    char secondImage [32];
    char outImage    [64];
    sprintf(firstImage, "frame%d.ppm", frame);
    sprintf(secondImage, "frame%d.ppm", frame+1);
    sprintf(outImage, "frame%d-%d.png", frame, frame+1);
    processImagePair(firstImage, secondImage, outImage);
  }
 
	return 0;
}

