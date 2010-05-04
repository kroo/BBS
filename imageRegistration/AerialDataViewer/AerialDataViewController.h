//
//  AerialDataViewController.h
//  AerialDataViewer
//
//  Created by Elliot Kroo on 5/3/10.
//  Copyright 2010 __MyCompanyName__. All rights reserved.
//

#import <Cocoa/Cocoa.h>
#import "AerialDataView.h"

struct ImageRecord {
  char name[32];
  float loc[9];
};

@interface AerialDataViewController : NSViewController <NSTableViewDelegate, NSTableViewDataSource> {
  NSTableView *table;
}

-(void) loadDatafile:(NSString *)file;

@property(assign) IBOutlet NSTableView *table;

@end
