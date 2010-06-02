// from http://dasl.mem.drexel.edu/~noahKuntz/openCVTut9.html#Step%201

#include <cv.h>
#include <highgui.h>
extern "C" {
#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
#include <libswscale/swscale.h>
#include <libavutil/avutil.h> // include the header!
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

const int MAX_CORNERS = 10;
static IplImage* trans_image = 0; // draw everything onto a shared canvas

// void processImagePair(const char *file1, const char *file2, CvVideoWriter *out, struct CvMat *currentOrientation) {
void processImagePair(int num, IplImage *imgAcolor, IplImage *imgBcolor, CvVideoWriter *out, struct CvMat *currentOrientation) {
	CvSize img_sz = cvGetSize( imgAcolor );

  // Load two images and allocate other structures
	IplImage* imgA = cvCreateImage(img_sz, 8, 1);//cvLoadImage(file1, CV_LOAD_IMAGE_GRAYSCALE);
	IplImage* imgB = cvCreateImage(img_sz, 8, 1);

  cvCvtColor(imgAcolor, imgA, CV_RGB2GRAY);
  cvCvtColor(imgBcolor, imgB, CV_RGB2GRAY);
 
	int win_size = 15;
  
	// Get the features for tracking
	IplImage* eig_image = cvCreateImage( img_sz, IPL_DEPTH_32F, 1 );
	IplImage* tmp_image = cvCreateImage( img_sz, IPL_DEPTH_32F, 1 );
 
	int corner_count = MAX_CORNERS;
	CvPoint2D32f* cornersA = new CvPoint2D32f[ MAX_CORNERS ];
 
	cvGoodFeaturesToTrack( imgA, eig_image, tmp_image, cornersA, &corner_count,
		0.05, 3.0, 0, 3, 0, 0.04 );
 
  fprintf(stderr, "frame %d: Corner count = %d\n", num, corner_count);
 
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
	// Find a homography based on the gradient
   CvMat cornersAMat = cvMat(1, corner_count, CV_32FC2, cornersA);
   CvMat cornersBMat = cvMat(1, corner_count, CV_32FC2, cornersB);
   cvFindHomography(&cornersAMat, &cornersBMat, transform, CV_RANSAC, 15, NULL);

   cvInvert(transform, invTransform);
   cvMatMul(currentOrientation, invTransform, currentOrientation);

   CvMat *tf_scaled_offset = cvCloneMat(currentOrientation);
   CvMat *scaled_down = cvCreateMat(3,3, CV_32FC1);
   cvSetIdentity(scaled_down);
   // cvSet2D(scaled_down, 0,0, cvScalar(0.5)); // 0.5 0.0 300
   // cvSet2D(scaled_down, 1,1, cvScalar(0.5)); // 0.0 0.5 300
   cvSet2D(scaled_down, 2,2, cvScalar(2.0)); // 0.0 0.5 300
   cvSet2D(scaled_down, 0,2, cvScalar(200));
   cvSet2D(scaled_down, 1,2, cvScalar(200));
   PrintMat(scaled_down);
   cvMatMul(scaled_down, currentOrientation, tf_scaled_offset);

   // save the translated image
   if (!trans_image) trans_image = cvCreateImage( img_sz, 8, 3 );
   cvWarpPerspective(imgBcolor, trans_image, tf_scaled_offset, CV_INTER_CUBIC);

   printf("%d:\n", num);
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
  
  
}


#if 0

int main(int argc, char *argv[]) {
  IplImage* src;
  if( argc == 2 && (src=cvLoadImage(argv[1], 0))!= 0)
  {
    calcNecessaryImageRotation(src);
  }
}
#endif
#if 0
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

static IplImage* lastImage = 0;
static bool firstTime = true;
// to change this, run this script (in python)!
// print "static unsigned char gamma_mapping[] = {", ", ".join(["%d" % int(float(i/256.) ** (2.2) * 256) for i in range(0,256)]), "};"
static unsigned char gamma_mapping[] = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 3, 3, 3, 3, 3, 4, 4, 4, 4, 5, 5, 5, 5, 6, 6, 6, 7, 7, 7, 8, 8, 8, 9, 9, 9, 10, 10, 10, 11, 11, 12, 12, 12, 13, 13, 14, 14, 15, 15, 16, 16, 17, 17, 18, 18, 19, 19, 20, 20, 21, 22, 22, 23, 23, 24, 25, 25, 26, 26, 27, 28, 28, 29, 30, 30, 31, 32, 33, 33, 34, 35, 36, 36, 37, 38, 39, 39, 40, 41, 42, 43, 44, 44, 45, 46, 47, 48, 49, 50, 51, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 70, 71, 72, 73, 74, 75, 76, 77, 78, 80, 81, 82, 83, 84, 86, 87, 88, 89, 91, 92, 93, 94, 96, 97, 98, 100, 101, 102, 104, 105, 106, 108, 109, 110, 112, 113, 115, 116, 117, 119, 120, 122, 123, 125, 126, 128, 129, 131, 132, 134, 135, 137, 139, 140, 142, 143, 145, 147, 148, 150, 152, 153, 155, 157, 158, 160, 162, 163, 165, 167, 169, 170, 172, 174, 176, 177, 179, 181, 183, 185, 187, 188, 190, 192, 194, 196, 198, 200, 202, 204, 206, 208, 210, 212, 214, 216, 218, 220, 222, 224, 226, 228, 230, 232, 234, 236, 238, 240, 242, 245, 247, 249, 251, 253 };
static CvMat *gammaMappingMat = 0;
void SetUpProcessing() {
  gammaMappingMat = cvCreateMatHeader( 1, 256, CV_8UC1 );
  cvSetData( gammaMappingMat, gamma_mapping, 0 );
}

void ProcessImage(AVFrame *pFrame, int width, int height, int frameno, CvMat *orientation, CvVideoWriter *writer) {
  if(!gammaMappingMat) SetUpProcessing();
  
  // set up an IplImage from the AVFrame
  IplImage* myIplImage = cvCreateImageHeader(cvSize(width,height), IPL_DEPTH_8U, 3);
  void *ptr = (void *)(pFrame->data[0]);
  cvSetData(myIplImage,ptr,width*3);
  
  // duplicate the image, to apply a gamma mapping
  IplImage* adjustedImage = cvCloneImage(myIplImage);
  
  // cvLUT(myIplImage, adjustedImage, gammaMappingMat);
    
  // process image:
  if(lastImage) {
    // we have an image pair to process
    printf("lastImage: %p\n", lastImage);
    // firstTime = false;
    
    processImagePair(frameno, lastImage, adjustedImage, writer, orientation);
    
    // cvSaveImage("output_image.png", adjustedImage);
    printf("processed: %d\n", frameno);
    
  }

  // clean up
  cvReleaseImageHeader(&myIplImage);
  if(lastImage)
    cvReleaseImage(&lastImage);
  lastImage = adjustedImage;
}


int ReadVideo(const char *filename) {
    AVFormatContext *pFormatCtx;
    int             i, videoStream;
    AVCodecContext  *pCodecCtx;
    AVCodec         *pCodec;
    AVFrame         *pFrame; 
    AVFrame         *pFrameBGR;
    int             numBytes;
    uint8_t         *buffer;

    int64_t firstFrame = 22560;
    int64_t lastFrame = 26640;

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
    
    
    writer=cvCreateVideoWriter("/Users/elliot/Desktop/out.avi",CV_FOURCC('P','I','M','1'),
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
          ProcessImage(pFrameBGR, pCodecCtx->width, pCodecCtx->height, i, orientation, writer);
        else if(i>(lastFrame-firstFrame)) break;
    }
    
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

int main (int argc, char const *argv[])
{
  if(argc > 1)
    ReadVideo(argv[1]);
  else
    printf("usage: bbs_registration VideoFile.avi");
  return 0;
}