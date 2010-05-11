//
//  AerialDataView.m
//  AerialDataViewer
//
//  Created by Elliot Kroo on 5/3/10.
//  Copyright 2010 __MyCompanyName__. All rights reserved.
//

#import "AerialDataView.h"
#import "AerialPhotograph.h"

@implementation AerialDataView
@synthesize images;

//-(id)initWithCoder:(NSCoder *)aDecoder {
//  self = [self initWithFrame: [self frame]];
//  return self;
//}

- (id)initWithFrame:(NSRect)frame {
  self = [super initWithFrame:frame];
  if (self) {
    NSLog(@"init with frame");
    CGColorRef color = CGColorCreateGenericRGB(0.7, 0.7, 0.7, 0.0);
    [self setWantsLayer:YES];
    scale = 0.7;
    CALayer *layer = [[CALayer alloc] init];
    [[self layer] setBackgroundColor: color];
    self.images = [[NSMutableArray alloc] initWithCapacity:500];
//    [layer setTransform:CATransform3DTranslate(CATransform3DMakeScale(0.5, 0.5, 1.0), 0, 0, 0)];
    [layer setSublayerTransform:CATransform3DScale(CATransform3DMakeTranslation(offset.x, offset.y, 0), scale, -scale, 1.0)];
    [self setLayer:layer];
    CFRelease(color);
  }
  return self;
}


- (void)mouseDown:(NSEvent *)theEvent {
  mouseDown = [theEvent locationInWindow];
}


- (void)mouseDragged:(NSEvent *)theEvent {
  float x = offset.x + ([theEvent locationInWindow].x - mouseDown.x), 
        y = offset.y + ([theEvent locationInWindow].y - mouseDown.y);
  [self.layer setSublayerTransform:CATransform3DScale(CATransform3DMakeTranslation(x, y, 0), scale, -scale, 1.0)];
}

- (void)mouseUp:(NSEvent *)theEvent {
  offset.x += [theEvent locationInWindow].x - mouseDown.x;
  offset.y += [theEvent locationInWindow].y - mouseDown.y;
}


- (void)showImage:(NSInteger)row {
	NSLog(@"showing image: %d", row);
  NSImageView *img = [[NSImageView alloc] initWithFrame:NSMakeRect(0, 0, 1280, 720)];
  AerialPhotograph *image = [images objectAtIndex:row];
  
  [img setImage: image];
  [self addSubview: img];
  [img setWantsLayer: YES];
  
	NSLog(@"transform: %f %f", image.transform.m13, image.transform.m23);
	img.frame = CGRectMake(0,0, img.frame.size.width, img.frame.size.height);
	CATransform3D transform = image.transform;
  [img layer].transform = transform;

//  ////  sublayer.contents = (id)[image CGImageForProposedRect:nil context:nil hints:nil];
//  [img setLayer:sublayer];
//  [sublayer setFrame: CGRectMake(0,0, 1280, 720)];
//  [sublayer setBounds: [img frame]];
  NSLog(@"img setting up at %@", NSStringFromRect(img.frame));
  [self setNeedsDisplay:YES];
  
  drawnImages++;
  
}

- (void)drawRect:(NSRect)dirtyRect {
  CGContextRef myContext = [[NSGraphicsContext currentContext] graphicsPort];
  CGContextSetRGBFillColor (myContext, 0, 0, 0, 1);
  float width = 1280;
  float height = 720;
  
  CGContextSetLineWidth(myContext, 1.0);
  CGContextSetRGBStrokeColor(myContext, 0.3, 0.3, 0.3, 0.4);
     
  NSBezierPath* drawingPath = [NSBezierPath bezierPath];
  
  int i;
  int GRIDSIZE = 15;
  int w = dirtyRect.size.width;
  int h = dirtyRect.size.height;
  // Draw a grid
  // first the vertical lines
  for( i = 0 ; i <= w ; i=i+GRIDSIZE ) { [drawingPath moveToPoint:NSMakePoint(i+0.5, 0)]; [drawingPath lineToPoint:NSMakePoint(i+0.5, h)]; } // then the horizontal lines
  for( i = 0 ; i <= h ; i=i+GRIDSIZE ) { [drawingPath moveToPoint:NSMakePoint(0,i+0.5)]; [drawingPath lineToPoint:NSMakePoint(w, i+0.5)]; } // actually draw the grid
  [drawingPath stroke];
  
  
  CGContextStrokeRect(myContext, CGRectMake (0, [self frame].size.height - height, width, height ));  
}

- (void)addImage:(NSString *)path transform:(CATransform3D)transform {
  AerialPhotograph *image = [[[AerialPhotograph alloc] initWithContentsOfFile: path] autorelease];
  image.transform = (transform);
  
  [images addObject: image];
//  NSImageView *img = [[NSImageView alloc] initWithFrame:NSMakeRect(0, 0, 1280, 720)];
//  [img setWantsLayer: YES];
//  CALayer *sublayer = [img layer];//[[CALayer alloc] init];
////  [[self layer] addSublayer:sublayer];
//  sublayer.transform = transform;
//  sublayer.frame = CGRectMake(0,0,[image size].width, [image size].height);
////  sublayer.contents = (id)[image CGImageForProposedRect:nil context:nil hints:nil];
//  [img setImage: image];
//  [img setLayer:sublayer];
//  [self addSubview: img];
}

@end
