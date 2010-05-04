//
//  AerialDataViewerAppDelegate.h
//  AerialDataViewer
//
//  Created by Elliot Kroo on 5/2/10.
//  Copyright 2010 __MyCompanyName__. All rights reserved.
//

#import <Cocoa/Cocoa.h>
#import "AerialDataViewController.h"

@interface AerialDataViewerAppDelegate : NSObject <NSApplicationDelegate> {
  NSWindow *window;
  AerialDataViewController *viewController;
  NSString *datafilePath;
}

@property (assign) IBOutlet NSWindow *window;
@property (assign) IBOutlet AerialDataViewController *viewController;

@end
