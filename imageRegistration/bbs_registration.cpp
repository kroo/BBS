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
void ProcessImage(AVFrame *pFrame, int width, int height, int frameno) {
  IplImage* myIplImage = cvCreateImageHeader(cvSize(width,height), IPL_DEPTH_8U, 3);
  void *ptr = (void *)(pFrame->data[0]);
  cvSetData(myIplImage,ptr,width*3);
  
  // process image:
  if(lastImage && firstTime) {
    // we have an image pair to process
    printf("lastImage: %p\n", lastImage);
    firstTime = false;
  
    cvSaveImage("output_image.png", myIplImage);
  }

  // clean up
  if(lastImage)
    cvReleaseImageHeader(&lastImage);
  lastImage = myIplImage;
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

    int64_t firstFrame = 15000;
    int64_t lastFrame = 16000;

    // Register all formats and codecs
    av_register_all();

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
          ProcessImage(pFrameBGR, pCodecCtx->width, pCodecCtx->height, i);
        else if(i>(lastFrame-firstFrame)) break;
    }

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