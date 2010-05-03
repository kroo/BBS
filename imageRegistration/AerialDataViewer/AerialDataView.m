//
//  AerialDataView.m
//  AerialDataViewer
//
//  Created by Elliot Kroo on 5/3/10.
//  Copyright 2010 __MyCompanyName__. All rights reserved.
//

#import "AerialDataView.h"


@implementation AerialDataView

- (id)initWithFrame:(NSRect)frame {
  self = [super initWithFrame:frame];
  if (self) {
    [[self layer] setBackgroundColor: CGColorCreateGenericRGB(0.7, 0.7, 0.7, 0.7)];
  }
  return self;
}

- (void)drawRect:(NSRect)dirtyRect {

}

- (void)addImage:(NSString *)path transform:(CATransform3D)transform {
  NSImageView *img = [[NSImageView alloc] initWithFrame:NSMakeRect(0, 0, 1280, 720)];
  [img setImage: [NSImage imageNamed:path]];
  [[img layer] setTransform:transform];
  [self addSubview: img];
}

@end
