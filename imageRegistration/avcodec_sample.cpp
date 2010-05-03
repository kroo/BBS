// avcodec_sample.cpp

// A small sample program that shows how to use libavformat and libavcodec to
// read video from a file.
//
// Use
//
// g++ -o avcodec_sample avcodec_sample.cpp -lavformat -lavcodec -lz
//
// to build (assuming libavformat and libavcodec are correctly installed on
// your system).
//
// Run using
//
// avcodec_sample myvideofile.mpg
//
// to write the first five frames from "myvideofile.mpg" to disk in PPM
// format.

extern "C" {
#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
#include <libswscale/swscale.h>
#include <libavutil/avutil.h> // include the header!
}

#include <stdio.h>

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

void SaveFrame(AVFrame *pFrame, int width, int height, int iFrame)
{
    FILE *pFile;
    char szFilename[32];
    int  y;

    // Open file
    sprintf(szFilename, "frame%d.ppm", iFrame);
    pFile=fopen(szFilename, "wb");
    if(pFile==NULL)
        return;

    // Write header
    fprintf(pFile, "P6\n%d %d\n255\n", width, height);

    // Write pixel data
    for(y=0; y<height; y++)
        fwrite(pFrame->data[0]+y*pFrame->linesize[0], 1, width*3, pFile);

    // Close file
    fclose(pFile);
}

int main(int argc, char *argv[])
{
    AVFormatContext *pFormatCtx;
    int             i, videoStream;
    AVCodecContext  *pCodecCtx;
    AVCodec         *pCodec;
    AVFrame         *pFrame; 
    AVFrame         *pFrameRGB;
    int             numBytes;
    uint8_t         *buffer;

    // Register all formats and codecs
    av_register_all();

    // Open video file
    if(av_open_input_file(&pFormatCtx, argv[1], NULL, 0, NULL)!=0)
        return -1; // Couldn't open file

    // Retrieve stream information
    if(av_find_stream_info(pFormatCtx)<0)
        return -1; // Couldn't find stream information

    // Dump information about file onto standard error
    dump_format(pFormatCtx, 0, argv[1], false);

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
    pFrameRGB=avcodec_alloc_frame();
    if(pFrameRGB==NULL)
        return -1;

    // Determine required buffer size and allocate buffer
    numBytes=avpicture_get_size(PIX_FMT_RGB24, pCodecCtx->width,
        pCodecCtx->height);
    buffer=new uint8_t[numBytes];

    // Assign appropriate parts of buffer to image planes in pFrameRGB
    avpicture_fill((AVPicture *)pFrameRGB, buffer, PIX_FMT_RGB24,
        pCodecCtx->width, pCodecCtx->height);

    SwsContext *swsContext = sws_getContext(pCodecCtx->width, pCodecCtx->height, pCodecCtx->pix_fmt,
                                            pCodecCtx->width, pCodecCtx->height, PIX_FMT_RGB24, SWS_BICUBIC, NULL, NULL, NULL);
    

    // Read frames and save first five frames to disk
    i=0;
    while(GetNextFrame(pFormatCtx, pCodecCtx, videoStream, pFrame))
    {
      sws_scale(swsContext,
                pFrame->data, pFrame->linesize, 0, pCodecCtx->height, 
                pFrameRGB->data, pFrameRGB->linesize);

        // Save the frame to disk
        if(++i<=1000 && i>750)
            SaveFrame(pFrameRGB, pCodecCtx->width, pCodecCtx->height, i);
        else if(i>750) break;
    }

    // Free the RGB image
    delete [] buffer;
    av_free(pFrameRGB);

    // Free the YUV frame
    av_free(pFrame);

    // Close the codec
    avcodec_close(pCodecCtx);

    // Close the video file
    av_close_input_file(pFormatCtx);

    return 0;
}
