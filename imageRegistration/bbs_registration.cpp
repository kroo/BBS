// from http://dasl.mem.drexel.edu/~noahKuntz/openCVTut9.html#Step%201

#include <cv.h>
#include <highgui.h>
extern "C" {
#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
#include <libswscale/swscale.h>
#include <libavutil/avutil.h> // include the header!
}
#include <ctype.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>


extern "C" {
  double car_predict(void **attr, double *ret);
  double green_predict(void **attr, double *ret);
  double road_predict(void **attr, double *ret);
}

bool GetNextFrame(AVFormatContext *pFormatCtx, AVCodecContext *pCodecCtx, 
    int videoStream, AVFrame *pFrame)
{
    static AVPacket packet;
    static int      bytesRemaining=0;
    static uint8_t  *rawData;
    static bool     fFirstTime=true;
    int             bytesDecoded;
    int             frameFinished;

    // First time we're called, set packet.data to NULL to indicate it
    // doesn't have to be freed
    if(fFirstTime)
    {
        fFirstTime=false;
        packet.data=NULL;
    }

    // Decode packets until we have decoded a complete frame
    while(true)
    {
        // Work on the current packet until we have decoded all of it
        while(bytesRemaining > 0)
        {
            // Decode the next chunk of data
            bytesDecoded=avcodec_decode_video(pCodecCtx, pFrame,
                &frameFinished, rawData, bytesRemaining);

            // Was there an error?
            if(bytesDecoded < 0)
            {
                fprintf(stderr, "Error while decoding frame\n");
                return false;
            }

            bytesRemaining-=bytesDecoded;
            rawData+=bytesDecoded;

            // Did we finish the current frame? Then we can return
            if(frameFinished)
                return true;
        }

        // Read the next packet, skipping all packets that aren't for this
        // stream
        do
        {
            // Free old packet
            if(packet.data!=NULL)
                av_free_packet(&packet);

            // Read new packet
            if(av_read_packet(pFormatCtx, &packet)<0)
                goto loop_exit;
        } while(packet.stream_index!=videoStream);

        bytesRemaining=packet.size;
        rawData=packet.data;
    }

loop_exit:

    // Decode the rest of the last frame
    bytesDecoded=avcodec_decode_video(pCodecCtx, pFrame, &frameFinished, 
        rawData, bytesRemaining);

    // Free last packet
    if(packet.data!=NULL)
        av_free_packet(&packet);

    return frameFinished!=0;
}


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


static int rotated_image_count = 0;
CvMat *calcNecessaryImageRotation(IplImage *src) {
#define MAX_LINES 360
  CvMemStorage* storage = cvCreateMemStorage(0);
  CvSize img_sz = cvGetSize( src );

	IplImage* color_dst = cvCreateImage( img_sz, 8, 3 );
	IplImage* dst = cvCreateImage( img_sz, 8, 1 );
  CvSeq* lines = 0;
  float avg = 0;
  int count = 0;
  int i;
  
  cvCanny( src, dst, 50, 200, 3 );
  cvCvtColor( dst, color_dst, CV_GRAY2BGR );
	
  // cvSaveImage("canny.png", dst);
  
  lines = cvHoughLines2( dst,
                         storage,
                         CV_HOUGH_PROBABILISTIC,
                         1,
                         CV_PI/180,
                         80,
                         30,
                         10 );
  int nbins = 360;
  int *hist = (int*)malloc(sizeof(int) * nbins);
  bzero(hist, sizeof(int) * nbins);
  for( i = 0; i < lines->total; i++ )
  {
      CvPoint* line = (CvPoint*)cvGetSeqElem(lines,i);
      cvLine( color_dst, line[0], line[1], CV_RGB(255,0,0), 1, 8 );
      double angle = atan((double)(line[1].y-line[0].y) / (double)(line[1].x-line[0].x));
      // histogram from 0 -> 2 pi
      hist[(int)(angle * ((float)nbins / (2. * M_PI))) % nbins] ++;
      avg += angle;
      count ++;
      // printf("%f\n", );
  }
  
  int max=0;
  for(int i=0; i<nbins; i++) {
    printf("%2.5f\t", ((float)i) * 2.f * M_PI / (float)nbins);
    for(int v=0; v<hist[i]; v++) printf("x");
    printf("\n");
    if(hist[i] > hist[max]) max = i;
  }
  avg /= (float)count;
  
  // TODO(kroo): build up a smoothed histogram (cvHistogram)
  // TODO(kroo): find two peaks, assert that they are separated by roughly 90˚
  // TODO(kroo): find smallest rotation necessary to cause the lines to point straight up/down/left/right
  

  // cvSaveImage("hough.png", color_dst);
  // 
  // cvNamedWindow( "Hough Transform", 1 );
  // cvShowImage( "Hough Transform", color_dst );
  // 
  // cvWaitKey(0);
  // exit(0);
  float radians = ((float)max) * 2.f * M_PI / (float)nbins;
  printf("radians: %f\n", radians);
  
  CvMat *transform = cvCreateMat(2,3, CV_32FC1);
  CvMat *bigger = cvCreateMat(3,3, CV_32FC1);
  cvSetIdentity(transform);
  cvSetIdentity(bigger);

  transform = cv2DRotationMatrix(cvPoint2D32f(640.0, 360.0), radians, 1.0, transform);
  for(int y=0; y<2; y++) {
    for(int x=0; x<3; x++) {
      cvSet2D(bigger, y, x, cvGet2D(transform, y, x));
    }
  }
  PrintMat(transform);
  PrintMat(bigger);


  char str[1024];
  sprintf(str, "rotated_image_%d.png", rotated_image_count++);
  CvMat *rotation = bigger;//calcNecessaryImageRotation(imgB);
  // cvInvert(rotation, rotation);
  IplImage *rotatedImage = cvCreateImage(img_sz, 8, color_dst->nChannels);
  cvSetZero(rotatedImage);
  cvWarpPerspective(color_dst, rotatedImage, rotation, CV_INTER_CUBIC);
  cvSaveImage(str, rotatedImage);
  cvReleaseImage(&rotatedImage);

  PrintMat(rotation);  



  
  return bigger;
  // return transform;

}

const int MAX_CORNERS = 150;
static IplImage* trans_image = 0; // draw everything onto a shared canvas

// void processImagePair(const char *file1, const char *file2, CvVideoWriter *out, struct CvMat *currentOrientation) {
bool processImagePair(int num, IplImage *imgAcolor, IplImage *imgBcolor, IplImage *imgBcolorReal, CvVideoWriter *out, struct CvMat *currentOrientation) {
	CvSize img_sz = cvGetSize( imgAcolor );

  // Load two images and allocate other structures
	IplImage* imgA = cvCreateImage(img_sz, 8, 1);//cvLoadImage(file1, CV_LOAD_IMAGE_GRAYSCALE);
	IplImage* imgB = cvCreateImage(img_sz, 8, 1);

  if(imgAcolor->nChannels == 3) {
    cvCvtColor(imgAcolor, imgA, CV_RGB2GRAY);
    cvCvtColor(imgBcolor, imgB, CV_RGB2GRAY);
  } else {
    imgA = cvCloneImage(imgAcolor);
    imgB = cvCloneImage(imgBcolor);
    IplImage *imgBcolor2 = cvCreateImage(img_sz, 8, 3);
    cvCvtColor(imgBcolor, imgBcolor2, CV_GRAY2RGB);
    imgBcolor = imgBcolor2;
  }
 
	int win_size = 15;
  
	// Get the features for tracking
	IplImage* eig_image = cvCreateImage( img_sz, IPL_DEPTH_32F, 1 );
	IplImage* tmp_image = cvCreateImage( img_sz, IPL_DEPTH_32F, 1 );
 
	int corner_count = MAX_CORNERS;
	CvPoint2D32f* cornersA = new CvPoint2D32f[ MAX_CORNERS ];
 
	cvGoodFeaturesToTrack( imgA, eig_image, tmp_image, cornersA, &corner_count,
		0.05, 3.0, 0, 3, 0, 0.04 );
 
  // fprintf(stderr, "frame %d: Corner count = %d\n", num, corner_count);
 
	cvFindCornerSubPix( imgA, cornersA, corner_count, cvSize( win_size, win_size ),
		cvSize( -1, -1 ), cvTermCriteria( CV_TERMCRIT_ITER | CV_TERMCRIT_EPS, 50, 0.03 ) );
 
	// Call Lucas Kanade algorithm
	char features_found[ MAX_CORNERS ];
	float feature_errors[ MAX_CORNERS ];
 
	CvSize pyr_sz = cvSize( imgA->width+8, imgB->height/3 );
 
	IplImage* pyrA = cvCreateImage( pyr_sz, IPL_DEPTH_32F, 1 );
	IplImage* pyrB = cvCreateImage( pyr_sz, IPL_DEPTH_32F, 1 );
 
	CvPoint2D32f* cornersB = new CvPoint2D32f[ MAX_CORNERS ];
 
  // calcNecessaryImageRotation(imgA);
 
	cvCalcOpticalFlowPyrLK( imgA, imgB, pyrA, pyrB, cornersA, cornersB, corner_count, 
		cvSize( win_size, win_size ), 5, features_found, feature_errors,
		 cvTermCriteria( CV_TERMCRIT_ITER | CV_TERMCRIT_EPS, 20, 0.3 ), 0 );
 
  CvMat *transform = cvCreateMat(3,3, CV_32FC1);
  CvMat *invTransform = cvCreateMat(3,3, CV_32FC1);
  CvMat *tempCurrentOrientation = cvCreateMat(3,3,CV_32FC1);
  // Find a homography based on the gradient
  CvMat cornersAMat = cvMat(1, corner_count, CV_32FC2, cornersA);
  CvMat cornersBMat = cvMat(1, corner_count, CV_32FC2, cornersB);
  cvFindHomography(&cornersAMat, &cornersBMat, transform, CV_RANSAC, 15, NULL);

  cvInvert(transform, invTransform);
  // check if currentOrientation is modified significantly:

  // PrintMat(invTransform);
  cvMatMul(currentOrientation, invTransform, tempCurrentOrientation);
  if(cvmGet(tempCurrentOrientation, 0, 0) > 10.f ||
    cvmGet(tempCurrentOrientation, 2, 2) < 0.0f ||
    cvmGet(tempCurrentOrientation, 0,2) > 10000.f) {
    printf("Unstable mapping!! Skipping this frame.\n");
    return false;
  }

  cvCopy(tempCurrentOrientation, currentOrientation);

  CvMat *tf_scaled_offset = cvCloneMat(currentOrientation);
  CvMat *scaled_down = cvCreateMat(3,3, CV_32FC1);
  cvSetIdentity(scaled_down);
  // cvSet2D(scaled_down, 2,2, cvScalar(2.5)); // 0.0 0.5 300
  // cvSet2D(scaled_down, 0,2, cvScalar(800));
  // cvSet2D(scaled_down, 1,2, cvScalar(600));
  cvMatMul(scaled_down, currentOrientation, tf_scaled_offset);

  // save the translated image
  if (!trans_image) {
    trans_image = cvCreateImage( img_sz, 8, imgBcolor->nChannels );
    cvSetZero(trans_image);
  }

  // IplImage *temp_image = cvCreateImage(img_sz, 8, 3);
  // cvWarpPerspective(imgBcolorReal, temp_image, tf_scaled_offset, CV_INTER_CUBIC+CV_WARP_FILL_OUTLIERS);
  // calcNecessaryImageRotation(temp_image);
  cvWarpPerspective(imgBcolorReal, trans_image, tf_scaled_offset, CV_INTER_CUBIC);
  printf("\n\n%d:\n", num);
  PrintMat(invTransform);
  calcNecessaryImageRotation(imgB);
  // PrintMat(currentOrientation);

  // cvSaveImage(out, trans_image);
  cvWriteFrame(out, trans_image);

  cvReleaseImage(&eig_image);
  cvReleaseImage(&tmp_image);  
  // cvReleaseImage(&trans_image);
  cvReleaseImage(&imgA);
  cvReleaseImage(&imgB);
  // cvReleaseImage(&imgBcolor);
  cvReleaseImage(&pyrA);
  cvReleaseImage(&pyrB);
  
  cvReleaseData(transform);
  delete [] cornersA;
  delete [] cornersB;
  
  return true;
}

// r,g,b values are from 0 to 1
// h = [0,360], s = [0,1], v = [0,1]
//		if s == 0, then h = -1 (undefined)
void RGBtoHSV2( double r, double g, double b, double *h, double *s, double *v ) {
  *v = fmax(r, fmax(g, b));
  *s = *v - fmin(r, fmin(g, b));
  if (*s == 0.f) *s = 1.f;
  if (r == *v) *h = (g - b) / *s;
  if (g == *v) *h = 2.f + (b - r) / *s;
  if (b == *v) *h = 4.f + (r - g) / *s;
  *h /= 6.f;
  if (*h <  0.0)  *h += 1.0;
  if (*s == 0.0) *h = 0.0;
  if (*v != 0) {
    *s /= *v;
  } else {
    *s = 0.f;
  }
}

// r,g,b values are from 0 to 1
// h = [0,360], s = [0,1], v = [0,1]
//		if s == 0, then h = -1 (undefined)
void RGBtoHSV( double r, double g, double b, double *h, double *s, double *v )
{
  float min, max, delta;
  min = fmin( fmin( r, g ), b );
  max = fmax( r, fmax( g, b ));
  *v = max;        // v
  delta = max - min;
  if( max != 0 )
    *s = delta / max;    // s
  else {
    // r = g = b = 0    // s = 0, v is undefined
    *s = 0;
    *h = -1;
    return;
  }
  if( r == max )
    *h = ( g - b ) / delta;    // between yellow & magenta
  else if( g == max )
    *h = 2 + ( b - r ) / delta;  // between cyan & yellow
  else
    *h = 4 + ( r - g ) / delta;  // between magenta & cyan
  *h *= 60;        // degrees
  if( *h < 0 )
    *h += 360;
}


static unsigned char gamma_mapping[] = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 3, 3, 3, 3, 3, 4, 4, 4, 4, 5, 5, 5, 5, 6, 6, 6, 7, 7, 7, 8, 8, 8, 9, 9, 9, 10, 10, 10, 11, 11, 12, 12, 12, 13, 13, 14, 14, 15, 15, 16, 16, 17, 17, 18, 18, 19, 19, 20, 20, 21, 22, 22, 23, 23, 24, 25, 25, 26, 26, 27, 28, 28, 29, 30, 30, 31, 32, 33, 33, 34, 35, 36, 36, 37, 38, 39, 39, 40, 41, 42, 43, 44, 44, 45, 46, 47, 48, 49, 50, 51, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 70, 71, 72, 73, 74, 75, 76, 77, 78, 80, 81, 82, 83, 84, 86, 87, 88, 89, 91, 92, 93, 94, 96, 97, 98, 100, 101, 102, 104, 105, 106, 108, 109, 110, 112, 113, 115, 116, 117, 119, 120, 122, 123, 125, 126, 128, 129, 131, 132, 134, 135, 137, 139, 140, 142, 143, 145, 147, 148, 150, 152, 153, 155, 157, 158, 160, 162, 163, 165, 167, 169, 170, 172, 174, 176, 177, 179, 181, 183, 185, 187, 188, 190, 192, 194, 196, 198, 200, 202, 204, 206, 208, 210, 212, 214, 216, 218, 220, 222, 224, 226, 228, 230, 232, 234, 236, 238, 240, 242, 245, 247, 249, 251, 253 };
static CvMat *gammaMappingMat = 0;

IplImage *ProcessColors(IplImage *image) {
  // calls double predict(void **attr, double *ret)
  CvSize img_sz = cvGetSize( image );
  
  for (int y=0; y<img_sz.height; y++) {
    uchar* ptr = (uchar*) (image->imageData + y * image->widthStep);
    
    for(int x=0; x<img_sz.width; x++) {
      
      // opencv stores images in "BGR" :)
      double b = ((double)ptr[x*3+0]) / 255.f;
      double g = ((double)ptr[x*3+1]) / 255.f;
      double r = ((double)ptr[x*3+2]) / 255.f;
      
      double h, s, v;
      RGBtoHSV2(r,g,b,&h,&s,&v);
      
      double car_results[] = {0, 0};
      double green_results[] = {0, 0};
      double road_results[] = {0, 0};
      
      double *attr[] = {&h, &s, &v};      
      double car_result = car_predict((void **)attr, car_results);
      double green_result = green_predict((void **)attr, green_results);
      double road_result = road_predict((void **)attr, road_results);
    
      double score = fmax(0.f,car_results[1]+1.0f) - fmax(0.f, 2.0f+green_results[1]) - fmax(0.f, 2.0f+road_results[1]);
      uint8_t color = (uint8_t)(fmax(0.f, fmin(255.f, 255.f * score)));
      ptr[x*3+0] = color;
      ptr[x*3+1] = color;
      ptr[x*3+2] = color;
    }
  }

  return image;
}

static IplImage* lastImage = 0;
static bool firstTime = true;
// to change this, run this script (in python)!
// print "static unsigned char gamma_mapping[] = {", ", ".join(["%d" % int(float(i/256.) ** (2.2) * 256) for i in range(0,256)]), "};"
void SetUpProcessing() {
  gammaMappingMat = cvCreateMatHeader( 1, 256, CV_8UC1 );
  cvSetData( gammaMappingMat, gamma_mapping, 0 );
}

void ProcessImage(AVFrame *pFrame, int width, int height, int frameno, CvMat *orientation, CvVideoWriter *writer, bool detect_cars) {
  if(!gammaMappingMat) SetUpProcessing();
  
  // set up an IplImage from the AVFrame
  IplImage* myIplImage = cvCreateImageHeader(cvSize(width,height), IPL_DEPTH_8U, 3);
  void *ptr = (void *)(pFrame->data[0]);
  cvSetData(myIplImage,ptr,width*3);
  
  // duplicate the image, to apply a gamma mapping
  IplImage* originalColorImage = cvCloneImage(myIplImage);
  IplImage* adjustedImage = cvCloneImage(myIplImage);
  if(detect_cars)
    adjustedImage = ProcessColors(adjustedImage);
  
  // cvSaveImage("color_processed_image.png", adjustedImage);
  // exit(0);
  
  // cvLUT(myIplImage, adjustedImage, gammaMappingMat);
    
  // only save this frame if processImagePair returns true
  bool saveThisFrame = true;
  // process image:
  if(lastImage) {
    // we have an image pair to process    
    if(!processImagePair(frameno, lastImage, originalColorImage, adjustedImage, writer, orientation)) {
      saveThisFrame = false;
    }
  }
  
  // clean up
  cvReleaseImage(&adjustedImage);
  cvReleaseImageHeader(&myIplImage);
  if(lastImage && saveThisFrame)
    cvReleaseImage(&lastImage);
  if(saveThisFrame)
    lastImage = originalColorImage;
}


int ReadVideo(const char *filename, int firstFrame_arg, int lastFrame_arg, const char *outputfile, bool detect_cars) {
    AVFormatContext *pFormatCtx;
    int             i, videoStream;
    AVCodecContext  *pCodecCtx;
    AVCodec         *pCodec;
    AVFrame         *pFrame; 
    AVFrame         *pFrameBGR;
    int             numBytes;
    uint8_t         *buffer;

    // int64_t firstFrame = 30 * 60 * 17;
    int64_t firstFrame = 22560;
    if(firstFrame_arg != -1) firstFrame = 0+firstFrame_arg;
    int64_t lastFrame = 26640;
    if(lastFrame_arg != -1) lastFrame = 0+lastFrame_arg;

    printf("Initting video output...\n");
    int frame = 11;
    CvMat *orientation = cvCreateMat(3,3,CV_32FC1);
    cvSetIdentity(orientation);
    CvVideoWriter *writer = 0;
    int isColor = 1;
    int fps     = 30;
    int frameW  = 1280;
    int frameH  = 720;
    
    // Register all formats and codecs
    av_register_all();
    
    writer=cvCreateVideoWriter(outputfile,CV_FOURCC('P','I','M','1'),
      fps,cvSize(frameW,frameH),isColor);
    printf("finished: %p\n", writer);

    // Open video file
    if(av_open_input_file(&pFormatCtx, filename, NULL, 0, NULL)!=0)
        return -1; // Couldn't open file

    // Retrieve stream information
    if(av_find_stream_info(pFormatCtx)<0)
        return -1; // Couldn't find stream information

    // Dump information about file onto standard error
    dump_format(pFormatCtx, 0, filename, false);

    // Find the first video stream
    videoStream=-1;
    for(i=0; i<pFormatCtx->nb_streams; i++)
        if(pFormatCtx->streams[i]->codec->codec_type==CODEC_TYPE_VIDEO)
        {
            videoStream=i;
            break;
        }
    if(videoStream==-1)
        return -1; // Didn't find a video stream

    // Get a pointer to the codec context for the video stream
    pCodecCtx=pFormatCtx->streams[videoStream]->codec;

    // Find the decoder for the video stream
    pCodec=avcodec_find_decoder(pCodecCtx->codec_id);
    if(pCodec==NULL)
        return -1; // Codec not found

    // Inform the codec that we can handle truncated bitstreams -- i.e.,
    // bitstreams where frame boundaries can fall in the middle of packets
    if(pCodec->capabilities & CODEC_CAP_TRUNCATED)
        pCodecCtx->flags|=CODEC_FLAG_TRUNCATED;

    // Open codec
    if(avcodec_open(pCodecCtx, pCodec)<0)
        return -1; // Could not open codec

    // Hack to correct wrong frame rates that seem to be generated by some 
    // codecs
    // if(pCodecCtx->frame_rate>1000 && pCodecCtx->frame_rate_base==1)
    //     pCodecCtx->frame_rate_base=1000;

    // Allocate video frame
    pFrame=avcodec_alloc_frame();

    // Allocate an AVFrame structure
    pFrameBGR=avcodec_alloc_frame();
    if(pFrameBGR==NULL)
        return -1;

    // Determine required buffer size and allocate buffer
    numBytes=avpicture_get_size(PIX_FMT_BGR24, pCodecCtx->width,
        pCodecCtx->height);
    buffer=new uint8_t[numBytes];

    // Assign appropriate parts of buffer to image planes in pFrameBGR
    avpicture_fill((AVPicture *)pFrameBGR, buffer, PIX_FMT_BGR24,
        pCodecCtx->width, pCodecCtx->height);

    SwsContext *swsContext = sws_getContext(pCodecCtx->width, pCodecCtx->height, pCodecCtx->pix_fmt,
                                            pCodecCtx->width, pCodecCtx->height, PIX_FMT_BGR24, SWS_BICUBIC, NULL, NULL, NULL);
    

    {
      // seek to middle of video
      printf("attempting to seek to %lld\n", firstFrame * AV_TIME_BASE / 30);
      int result = av_seek_frame(pFormatCtx, -1, firstFrame * AV_TIME_BASE / 30, 0);
      printf("result: %d\n", result);
    }
    
    // Read frames and save first five frames to disk
    i=0;
        
    while(GetNextFrame(pFormatCtx, pCodecCtx, videoStream, pFrame))
    {
      sws_scale(swsContext,
                pFrame->data, pFrame->linesize, 0, pCodecCtx->height, 
                pFrameBGR->data, pFrameBGR->linesize);

        if(i%100 == 0) printf("frame %lld reached\n", i+firstFrame);
        // Save the frame to disk
        if(++i<=lastFrame-firstFrame && i>0)
          ProcessImage(pFrameBGR, pCodecCtx->width, pCodecCtx->height, i, orientation, writer, detect_cars);
        else if(i>(lastFrame-firstFrame)) break;
    }
    
    cvSaveImage("Final_Composite.png", trans_image);
    
    cvReleaseVideoWriter(&writer);

    // Free the BGR image
    delete [] buffer;
    av_free(pFrameBGR);

    // Free the YUV frame
    av_free(pFrame);

    // Close the codec
    avcodec_close(pCodecCtx);

    // Close the video file
    av_close_input_file(pFormatCtx);

    return 0;
}

int main (int argc, char **argv)
{
  char *filename = 0;
  char *outputfile = 0;
  int firstFrame = -1;
  int lastFrame = -1;
  int i=0, c=0, index=0;
  opterr = 0;
  bool die = false, detect_cars = false;
  
  while ((c = getopt (argc, argv, "cs:e:")) != -1)
    switch (c)
  {
    case 's':
    firstFrame = atoi(optarg);
    break;
    case 'c':
    detect_cars = true;
    case 'e':
    lastFrame = atoi(optarg);
    break;
    case '?':
    if (optopt == 's' || optopt == 'e')
      fprintf (stderr, "Option -%c requires an argument.\n", optopt);
    else if (isprint (optopt))
      fprintf (stderr, "Unknown option `-%c'.\n", optopt);
    else
      fprintf (stderr, "Unknown option character `\\x%x'.\n", optopt);
    return 1;
    default:
    die = true;
    break;
  }
  
  for (index = optind; index < argc; index++, i++) {
    switch(i) {
      case 0: filename = argv[index]; break;
      case 1: outputfile = argv[index]; break;
      default:
      die=true;
      break;
    }
  }  
  
  if(!filename || die) {
    fprintf(stderr, "Usage: bbs_registration [-s startFrame] [-e endFrame] [filename] [outputfile]\n");
    exit(-1);
  } else {
    fprintf(stderr, "Start: %d End: %d Input: %s Output: %s\n", firstFrame, lastFrame, filename, outputfile);
  }
  if(!outputfile) outputfile = "output.avi";
  ReadVideo(filename, firstFrame, lastFrame, outputfile, detect_cars);
  return 0;
}