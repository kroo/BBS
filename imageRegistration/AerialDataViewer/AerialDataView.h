//
//  AerialDataView.h
//  AerialDataViewer
//
//  Created by Elliot Kroo on 5/3/10.
//  Copyright 2010 __MyCompanyName__. All rights reserved.
//

#import <Cocoa/Cocoa.h>


@interface AerialDataView : NSView {
  NSMutableArray *images;
  CGPoint offset;
  CGPoint mouseDown;
  float scale;
  float drawnImages;

}
- (void)addImage:(NSString *)path transform:(CATransform3D)transform;
- (void)showImage:(NSInteger)row;

@property(retain) NSMutableArray *images;

@end
