//
//  AerialDataViewController.m
//  AerialDataViewer
//
//  Created by Elliot Kroo on 5/3/10.
//  Copyright 2010 __MyCompanyName__. All rights reserved.
//

#import "AerialDataViewController.h"


@implementation AerialDataViewController

@synthesize table;

- (NSInteger)numberOfRowsInTableView:(NSTableView *)aTableView {
  return [[(AerialDataView *)self.view images] count];
}

- (id)tableView:(NSTableView *)aTableView objectValueForTableColumn:(NSTableColumn *)aTableColumn row:(NSInteger)rowIndex {
  return [NSString stringWithFormat:@"Image %d", rowIndex];
}

- (BOOL)tableView:(NSTableView *)aTableView shouldSelectRow:(NSInteger)rowIndex {
  [(AerialDataView *)self.view showImage: rowIndex];
  return YES;
}




-(void) loadDatafile:(NSString *)file {
  NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
	//  NSLog(@"loading data at %@", file);
  NSString *dir = [file stringByDeletingLastPathComponent];
  NSData *data = [NSData dataWithContentsOfFile:file];
  for(int i=0; i<[data length]; i+= sizeof(struct ImageRecord)) {
    struct ImageRecord rec;
		[data getBytes:&rec range:NSMakeRange(i, sizeof(struct ImageRecord))];
		//    NSLog(@"image record found: %s", rec.name);
    CATransform3D transform;
    transform.m11 = rec.loc[0];  transform.m12 = -rec.loc[1]; transform.m13 =  rec.loc[2]; transform.m14 = 0;
    transform.m21 = -rec.loc[3]; transform.m22 = -rec.loc[4]; transform.m23 = -rec.loc[5]; transform.m24 = 0;
    transform.m31 = rec.loc[6];  transform.m32 = -rec.loc[7]; transform.m33 =  rec.loc[8]; transform.m34 = 0;
    transform.m41 = 0;           transform.m42 = 0;           transform.m43 =  0;          transform.m44 = 1;
		
		NSLog(@"transform: \n %f %f %f\n %f %f %f\n %f %f %f", 
					transform.m11, transform.m12, transform.m13,
					transform.m21, transform.m22, transform.m23,
					transform.m31, transform.m32, transform.m33);
    
    [(AerialDataView *)self.view addImage:[dir stringByAppendingPathComponent: [NSString stringWithCString:rec.name encoding: NSUTF8StringEncoding]] transform:transform];
    
    [table reloadData];
    
  }
  [pool drain];
}


@end
