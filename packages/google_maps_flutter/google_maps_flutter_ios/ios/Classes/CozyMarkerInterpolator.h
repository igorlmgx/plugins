//
//  CozyMarkerInterpolator.h
//  Pods
//
//  Created by Pietro Domingues on 26/09/23.
//
#import "CozyMarkerElements.h"

@interface CozyMarkerInterpolator : NSObject
// @property UIFont *font;
// - (instancetype)initWithFont:(UIFont *)font;
- (CozyMarkerElements *)getInterpolatedMarkerElementsWithStart:(CozyMarkerElements *)start end:(CozyMarkerElements *)end step:(CGFloat)step;
@end
