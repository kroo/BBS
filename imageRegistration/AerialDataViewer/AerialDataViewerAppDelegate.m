//
//  AerialDataViewerAppDelegate.m
//  AerialDataViewer
//
//  Created by Elliot Kroo on 5/2/10.
//  Copyright 2010 __MyCompanyName__. All rights reserved.
//

#import "AerialDataViewerAppDelegate.h"
#import "PPMImageRep.h"

@implementation AerialDataViewerAppDelegate

@synthesize window, viewController;

- (void)applicationDidFinishLaunching:(NSNotification *)aNotification {
  [NSImageRep registerImageRepClass:[PPMImageRep class]];

	NSOpenPanel *panel = [NSOpenPanel openPanel];
  [panel setAllowedFileTypes: [NSArray arrayWithObject:@"bin"]];
  int result = [panel runModal];
  NSLog(@"panel responded: %@, %d", [panel URL], result);
  if (result) {
    datafilePath = [[panel URL] path];
    [NSThread detachNewThreadSelector:@selector(loadDatafile:) toTarget:viewController withObject:datafilePath];
//    [self loadDatafile: [[panel URL] path]];
  }
}

@end
