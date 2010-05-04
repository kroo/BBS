//
//  PPMImageRep.m
//  AerialDataViewer
//
//  Created by Elliot Kroo on 5/3/10.
//  Copyright 2010 __MyCompanyName__. All rights reserved.
//

#import "PPMImageRep.h"

static char *kCustomImageMagic = "P6";

@implementation PPMImageRep
+ (NSArray *)imageUnfilteredTypes {
	// This is a UTI
	return [NSArray arrayWithObjects:
          @"com.custom.imagetype",
          nil
          ];
}
+ (NSArray *)imageUnfilteredFileTypes {
	// This is a filename suffix
	return [NSArray arrayWithObjects:
          @"ppm",
          nil
          ];
}
+ (BOOL)canInitWithData:(NSData *)data {
  int minWidthChars = 1;
  int minHeightChars = 1;
  int minMaxValChars = 1;
  int spaceLen = 1;
  int newlineLen = 1;
  int headerLength = sizeof(kCustomImageMagic) + newlineLen + minWidthChars + spaceLen + minHeightChars + newlineLen + minMaxValChars + newlineLen;
  
	if ([data length] >= /*sizeof(CustomImageHeader)*/ headerLength) {
		const char *magic = [data bytes];
		if (memcmp(kCustomImageMagic, magic, sizeof(kCustomImageMagic)) == 0) {
			return YES;
		}
	}
	return NO;
}
+ (id)imageRepWithData:(NSData *)data {
	return [[[self alloc] initWithData:data] autorelease];
}
+ (id)imageRepWithContentsOfFile:(NSString *)filename {
	NSData *data = [NSData dataWithContentsOfFile:filename];
	return [[[self alloc] initWithData:data] autorelease];
}
+ (id)imageRepWithContentsOfURL:(NSURL *)aURL {
	NSData *data = [NSData dataWithContentsOfURL:aURL];
	return [[[self alloc] initWithData:data] autorelease];
}
- (id)initWithData:(NSData *)data {
	self = [super init];
	if (!self) {
		return nil;
	}
	
  int nx, ny, bps;
  
  char *loc = (char *)[data bytes];
  char *end = (char *)[data bytes] + ([data length] - 1);
  assert(loc[0] == 'P' && loc[1] == '6');
  int spp = 3; // if 'P6', has 3 channels
  
  while(loc!=end && *loc != '\n') loc++; loc ++; assert(loc!=end);
  
  sscanf(loc, "%d %d", &nx, &ny);

  while(loc!=end && *loc != '\n') loc++; loc ++; assert(loc!=end);

  sscanf(loc, "%d", &bps);

  while(loc!=end && *loc != '\n') loc++; loc ++; assert(loc!=end);

  if(bps < 256) bps = 8;
  else bps = 16;
  
  NSString* csp = NSDeviceRGBColorSpace;
  
  NSBitmapImageRep *image2 = NULL;
  image2 = [[NSBitmapImageRep alloc]
            initWithBitmapDataPlanes:NULL
            pixelsWide:nx pixelsHigh:ny bitsPerSample:bps
            samplesPerPixel:spp hasAlpha:NO isPlanar:NO
            colorSpaceName:csp
            bytesPerRow:nx*spp*(bps/8) bitsPerPixel:spp*bps ];

  if(image2) {
    memcpy([image2 bitmapData], loc, end-loc);
  }

  image = [image2 CGImage];
	
	int width = CGImageGetWidth(image);
	int height = CGImageGetHeight(image);
	if (width <= 0 || height <= 0) {
		NSLog(@"Invalid image size: Both width and height must be > 0");
		[self autorelease];
		return nil;
	}
	[self setPixelsWide:width];
	[self setPixelsHigh:height];
	[self setSize:NSMakeSize(width, height)];
	[self setColorSpaceName:NSDeviceRGBColorSpace];
	[self setBitsPerSample:8];
	[self setAlpha:YES];
	[self setOpaque:NO];
	
	return self;
}
- (void)dealloc {
	CGImageRelease(image);
	[super dealloc];
}
- (BOOL)draw {
	CGContextRef context = [[NSGraphicsContext currentContext] graphicsPort];
	if (!context || !image) {
		return NO;
	}
	NSSize size = [self size];
	CGContextDrawImage(context, CGRectMake(0, 0, size.width, size.height), image);
	return YES;
}

@end
